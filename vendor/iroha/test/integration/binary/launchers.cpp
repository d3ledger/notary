/**
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

#include "integration/binary/launchers.hpp"

#include <string>

#include <gtest/gtest.h>
#include "bindings/model_crypto.hpp"
#include "common/byteutils.hpp"

using namespace boost::process;

namespace binary_test {

  constexpr auto cTimeToKill = std::chrono::minutes(15);

  void Launcher::operator()(const std::string &example) {
    ipstream pipe;
    const auto &command = launchCommand(example);
    if (command.empty()) {
      FAIL() << "Launcher provided empty command";
    }
    child c(command, std_out > pipe);
    auto terminated = c.wait_for(cTimeToKill);
    if (not terminated) {
      c.terminate();
      FAIL() << "Child process was terminated because execution time limit "
                "has been exceeded";
    }
    readBinaries(pipe);
  }

  void Launcher::readBinaries(ipstream &stream) {
    transactions.clear();
    queries.clear();
    std::string packed_line;
    std::string raw_payload;
    while (stream and std::getline(stream, packed_line)
           and packed_line.size() > 1) {
      raw_payload = packed_line.substr(1);
      if (auto byte_string = iroha::hexstringToBytestring(raw_payload)) {
        auto binary_type = packed_line.at(0);
        switch (binary_type) {
          case 'K': {
            if (not admin_key) {
              admin_key = shared_model::bindings::ModelCrypto().fromPrivateKey(
                  raw_payload);
            }
            break;
          }
          case 'T': {
            iroha::protocol::Transaction proto_tx;
            if (proto_tx.ParseFromString(*byte_string)) {
              transactions.emplace_back(std::move(proto_tx));
            }
            break;
          }
          case 'Q': {
            iroha::protocol::Query proto_query;
            if (proto_query.ParseFromString(*byte_string)) {
              queries.emplace_back(std::move(proto_query));
            }
            break;
          }
        }  // switch (binary_type)
      }
    }
  }

  bool Launcher::initialized(const unsigned &transactions_expected,
                             const unsigned &queries_expected) {
    checkAsserts(transactions_expected, queries_expected);
    return admin_key and transactions.size() == transactions_expected
        and queries.size() == queries_expected;
  }

  void Launcher::checkAsserts(const unsigned &transactions_expected,
                              const unsigned &queries_expected) {
    ASSERT_TRUE(admin_key);
    ASSERT_EQ(transactions.size(), transactions_expected);
    ASSERT_EQ(queries.size(), queries_expected);
  }

  std::string PythonLauncher::launchCommand(const std::string &example) {
    std::stringstream s;
    // TODO, igor-egorov, 2018-06-30, IR-1488, avoid bash and use
    // boost::filesystem
    s << "bash -c \"${PYTHON_INTERPRETER} "
         "${ROOT_DIR}/example/python/permissions/"
      << example << ".py\"";
    return s.str();
  }

  std::string JavaLauncher::launchCommand(const std::string &example) {
    return "";
    // TODO, igor-egorov, 2018-06-20, IR-1389
  }

}  // namespace binary_test
