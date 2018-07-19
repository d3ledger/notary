/**
 * Copyright Soramitsu Co., Ltd. 2017 All Rights Reserved.
 * http://soramitsu.co.jp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY =KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <gflags/gflags.h>
#include <rapidjson/istreamwrapper.h>
#include <rapidjson/rapidjson.h>
#include <boost/filesystem.hpp>
#include <iostream>

#include "backend/protobuf/queries/proto_query.hpp"
#include "client.hpp"
#include "converters/protobuf/json_proto_converter.hpp"
#include "crypto/keys_manager_impl.hpp"
#include "grpc_response_handler.hpp"
#include "interactive/interactive_cli.hpp"
#include "model/converters/json_block_factory.hpp"
#include "model/converters/json_query_factory.hpp"
#include "model/converters/pb_block_factory.hpp"
#include "model/converters/pb_query_factory.hpp"
#include "model/converters/pb_transaction_factory.hpp"
#include "model/generators/block_generator.hpp"
#include "model/model_crypto_provider_impl.hpp"

// Account information
DEFINE_bool(new_account,
            false,
            "Generate and save locally new public/private keys");
DEFINE_string(account_name,
              "",
              "Name of the account. Must be unique in iroha network");
DEFINE_string(pass_phrase, "", "Account pass-phrase");
DEFINE_string(key_path, ".", "Path to user keys");

// Iroha peer to connect with
DEFINE_string(peer_ip, "0.0.0.0", "Address of the Iroha node");
DEFINE_int32(torii_port, 50051, "Port of Iroha's Torii");

// Send already signed and formed transaction to Iroha peer
DEFINE_string(json_transaction, "", "Transaction in json format");
// Send already signed and formed query to Iroha peer
DEFINE_string(json_query, "", "Query in json format");

// Genesis block generator:
DEFINE_bool(genesis_block,
            false,
            "Generate genesis block for new Iroha network");
DEFINE_string(genesis_transaction,
              "",
              "File with transaction in json format for the genesis block");
DEFINE_string(peers_address,
              "",
              "File with peers address for new Iroha network");

// Run iroha-cli in interactive mode
DEFINE_bool(interactive, true, "Run iroha-cli in interactive mode");

using namespace iroha::protocol;
using namespace iroha::model::generators;
using namespace iroha::model::converters;
using namespace iroha_cli::interactive;
namespace fs = boost::filesystem;

iroha::keypair_t *makeOldModel(const shared_model::crypto::Keypair &keypair) {
  return new iroha::keypair_t{
      iroha::pubkey_t::from_string(toBinaryString(keypair.publicKey())),
      iroha::privkey_t::from_string(toBinaryString(keypair.privateKey()))};
}

int main(int argc, char *argv[]) {
  gflags::ParseCommandLineFlags(&argc, &argv, true);
  gflags::ShutDownCommandLineFlags();
  auto logger = logger::log("CLI-MAIN");
  // Generate new genesis block now Iroha network
  if (FLAGS_genesis_block) {
    BlockGenerator generator;
    iroha::model::Transaction transaction;
    if (FLAGS_genesis_transaction.empty()) {
      if (FLAGS_peers_address.empty()) {
        logger->error("--peers_address is empty");
        return EXIT_FAILURE;
      }
      std::ifstream file(FLAGS_peers_address);
      std::vector<std::string> peers_address;
      std::copy(std::istream_iterator<std::string>(file),
                std::istream_iterator<std::string>(),
                std::back_inserter(peers_address));
      // Generate genesis block
      transaction = TransactionGenerator().generateGenesisTransaction(
          0, std::move(peers_address));
    } else {
      rapidjson::Document doc;
      std::ifstream file(FLAGS_genesis_transaction);
      rapidjson::IStreamWrapper isw(file);
      doc.ParseStream(isw);
      auto some_tx = JsonTransactionFactory{}.deserialize(doc);
      if (some_tx) {
        transaction = *some_tx;
      } else {
        logger->error(
            "Cannot deserialize genesis transaction (problem with file reading "
            "or illformed json?)");
        return EXIT_FAILURE;
      }
    }

    auto block = generator.generateGenesisBlock(0, {transaction});
    // Convert to json
    std::ofstream output_file("genesis.block");
    auto bl = shared_model::proto::Block(
        iroha::model::converters::PbBlockFactory().serialize(block));
    output_file << shared_model::converters::protobuf::modelToJson(bl);
    logger->info("File saved to genesis.block");
  }
  // Create new pub/priv key, register in Iroha Network
  else if (FLAGS_new_account) {
    if (FLAGS_account_name.empty()) {
      logger->error("No account name specified");
      return EXIT_FAILURE;
    }
    auto keysManager = iroha::KeysManagerImpl(FLAGS_account_name);
    if (not(FLAGS_pass_phrase.size() == 0
                ? keysManager.createKeys()
                : keysManager.createKeys(FLAGS_pass_phrase))) {
      logger->error("Keys already exist");
      return EXIT_FAILURE;
    }
    logger->info(
        "Public and private key has been generated in current directory");

  }
  // Send to Iroha Peer json transaction/query
  else if (not FLAGS_json_transaction.empty() or not FLAGS_json_query.empty()) {
    iroha_cli::CliClient client(FLAGS_peer_ip, FLAGS_torii_port);
    iroha_cli::GrpcResponseHandler response_handler;
    if (not FLAGS_json_transaction.empty()) {
      logger->info(
          "Send transaction to {}:{} ", FLAGS_peer_ip, FLAGS_torii_port);
      // Read from file
      std::ifstream file(FLAGS_json_transaction);
      std::string str((std::istreambuf_iterator<char>(file)),
                      std::istreambuf_iterator<char>());
      iroha::model::converters::JsonTransactionFactory serializer;
      auto doc = iroha::model::converters::stringToJson(str);
      if (not doc) {
        logger->error("Json has wrong format.");
        return EXIT_FAILURE;
      }
      auto tx_opt = serializer.deserialize(doc.value());
      if (not tx_opt) {
        logger->error("Json transaction has wrong format.");
        return EXIT_FAILURE;
      } else {
        auto tx = shared_model::proto::Transaction(
            iroha::model::converters::PbTransactionFactory().serialize(
                *tx_opt));
        response_handler.handle(client.sendTx(tx));
      }
    }
    if (not FLAGS_json_query.empty()) {
      logger->info("Send query to {}:{}", FLAGS_peer_ip, FLAGS_torii_port);
      std::ifstream file(FLAGS_json_query);
      std::string str((std::istreambuf_iterator<char>(file)),
                      std::istreambuf_iterator<char>());
      iroha::model::converters::JsonQueryFactory serializer;
      auto query_opt = serializer.deserialize(std::move(str));
      if (not query_opt) {
        logger->error("Json has wrong format.");
        return EXIT_FAILURE;
      } else {
        auto query = shared_model::proto::Query(
            *iroha::model::converters::PbQueryFactory().serialize(*query_opt));
        auto response = client.sendQuery(query);
        response_handler.handle(response);
      }
    }
  }
  // Run iroha-cli in interactive mode
  else if (FLAGS_interactive) {
    if (FLAGS_account_name.empty()) {
      logger->error("Specify your account name");
      return EXIT_FAILURE;
    }
    fs::path path(FLAGS_key_path);
    if (not fs::exists(path)) {
      logger->error("Path {} not found.", path.string());
      return EXIT_FAILURE;
    }
    iroha::KeysManagerImpl manager((path / FLAGS_account_name).string());
    auto keypair = FLAGS_pass_phrase.size() != 0
        ? manager.loadKeys(FLAGS_pass_phrase)
        : manager.loadKeys();
    if (not keypair) {
      logger->error(
          "Cannot load specified keypair, or keypair is invalid. Path: {}, "
          "keypair name: {}. Use --key_path with path of your keypair. \n"
          "Maybe wrong pass phrase (\"{}\")?",
          path.string(),
          FLAGS_account_name,
          FLAGS_pass_phrase);
      return EXIT_FAILURE;
    }
    // TODO 13/09/17 grimadas: Init counters from Iroha, or read from disk?
    // IR-334
    InteractiveCli interactiveCli(
        FLAGS_account_name,
        FLAGS_peer_ip,
        FLAGS_torii_port,
        0,
        std::make_shared<iroha::model::ModelCryptoProviderImpl>(
            *std::unique_ptr<iroha::keypair_t>(makeOldModel(*keypair))));
    interactiveCli.run();
  } else {
    logger->error("Invalid flags");
    return EXIT_FAILURE;
  }
  return 0;
}
