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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "main/application.hpp"
#include "ametsuchi/impl/postgres_ordering_service_persistent_state.hpp"
#include "ametsuchi/impl/wsv_restorer_impl.hpp"
#include "consensus/yac/impl/supermajority_checker_impl.hpp"
#include "multi_sig_transactions/gossip_propagation_strategy.hpp"
#include "multi_sig_transactions/mst_processor_impl.hpp"
#include "multi_sig_transactions/mst_processor_stub.hpp"
#include "multi_sig_transactions/mst_time_provider_impl.hpp"
#include "multi_sig_transactions/storage/mst_storage_impl.hpp"
#include "multi_sig_transactions/transport/mst_transport_grpc.hpp"

using namespace iroha;
using namespace iroha::ametsuchi;
using namespace iroha::simulator;
using namespace iroha::validation;
using namespace iroha::network;
using namespace iroha::synchronizer;
using namespace iroha::torii;
using namespace iroha::consensus::yac;

using namespace std::chrono_literals;

/**
 * Configuring iroha daemon
 */
Irohad::Irohad(const std::string &block_store_dir,
               const std::string &pg_conn,
               size_t torii_port,
               size_t internal_port,
               size_t max_proposal_size,
               std::chrono::milliseconds proposal_delay,
               std::chrono::milliseconds vote_delay,
               std::chrono::milliseconds load_delay,
               const shared_model::crypto::Keypair &keypair,
               bool is_mst_supported)
    : block_store_dir_(block_store_dir),
      pg_conn_(pg_conn),
      torii_port_(torii_port),
      internal_port_(internal_port),
      max_proposal_size_(max_proposal_size),
      proposal_delay_(proposal_delay),
      vote_delay_(vote_delay),
      load_delay_(load_delay),
      is_mst_supported_(is_mst_supported),
      keypair(keypair) {
  log_ = logger::log("IROHAD");
  log_->info("created");
  // Initializing storage at this point in order to insert genesis block before
  // initialization of iroha deamon
  initStorage();
}

/**
 * Initializing iroha daemon
 */
void Irohad::init() {
  // Recover VSW from the existing ledger to be sure it is consistent
  initWsvRestorer();
  restoreWsv();

  initPeerQuery();
  initCryptoProvider();
  initValidators();
  initOrderingGate();
  initSimulator();
  initBlockLoader();
  initConsensusGate();
  initSynchronizer();
  initPeerCommunicationService();
  initMstProcessor();

  // Torii
  initTransactionCommandService();
  initQueryService();
}

/**
 * Dropping iroha daemon storage
 */
void Irohad::dropStorage() {
  storage->dropStorage();
  ordering_service_storage_->resetState();
}

/**
 * Initializing iroha daemon storage
 */
void Irohad::initStorage() {
  auto storageResult = StorageImpl::create(block_store_dir_, pg_conn_);
  storageResult.match(
      [&](expected::Value<std::shared_ptr<ametsuchi::StorageImpl>> &_storage) {
        storage = _storage.value;
      },
      [&](expected::Error<std::string> &error) { log_->error(error.error); });

  PostgresOrderingServicePersistentState::create(pg_conn_).match(
      [&](expected::Value<
          std::shared_ptr<ametsuchi::PostgresOrderingServicePersistentState>>
              &_storage) { ordering_service_storage_ = _storage.value; },
      [&](expected::Error<std::string> &error) { log_->error(error.error); });

  log_->info("[Init] => storage", logger::logBool(storage));
}

void Irohad::resetOrderingService() {
  if (not ordering_service_storage_->resetState())
    log_->error("cannot reset ordering service storage");
}

bool Irohad::restoreWsv() {
  return wsv_restorer_->restoreWsv(*storage).match(
      [](iroha::expected::Value<void> v) { return true; },
      [&](iroha::expected::Error<std::string> &error) {
        log_->error(error.error);
        return false;
      });
}

/**
 * Initializing peer query interface
 */
std::unique_ptr<iroha::ametsuchi::PeerQuery> Irohad::initPeerQuery() {
  return std::make_unique<ametsuchi::PeerQueryWsv>(storage->getWsvQuery());
}

/**
 * Initializing crypto provider
 */
void Irohad::initCryptoProvider() {
  crypto_signer_ =
      std::make_shared<shared_model::crypto::CryptoModelSigner<>>(keypair);

  log_->info("[Init] => crypto provider");
}

/**
 * Initializing validators
 */
void Irohad::initValidators() {
  stateful_validator = std::make_shared<StatefulValidatorImpl>();
  chain_validator = std::make_shared<ChainValidatorImpl>(
      std::make_shared<consensus::yac::SupermajorityCheckerImpl>());

  log_->info("[Init] => validators");
}

/**
 * Initializing ordering gate
 */
void Irohad::initOrderingGate() {
  ordering_gate = ordering_init.initOrderingGate(initPeerQuery(),
                                                 max_proposal_size_,
                                                 proposal_delay_,
                                                 ordering_service_storage_,
                                                 storage->getBlockQuery());
  log_->info("[Init] => init ordering gate - [{}]",
             logger::logBool(ordering_gate));
}

/**
 * Initializing iroha verified proposal creator and block creator
 */
void Irohad::initSimulator() {
  simulator = std::make_shared<Simulator>(ordering_gate,
                                          stateful_validator,
                                          storage,
                                          storage->getBlockQuery(),
                                          crypto_signer_);

  log_->info("[Init] => init simulator");
}

/**
 * Initializing block loader
 */
void Irohad::initBlockLoader() {
  block_loader =
      loader_init.initBlockLoader(initPeerQuery(), storage->getBlockQuery());

  log_->info("[Init] => block loader");
}

/**
 * Initializing consensus gate
 */
void Irohad::initConsensusGate() {
  consensus_gate = yac_init.initConsensusGate(initPeerQuery(),
                                              simulator,
                                              block_loader,
                                              keypair,
                                              vote_delay_,
                                              load_delay_);

  log_->info("[Init] => consensus gate");
}

/**
 * Initializing synchronizer
 */
void Irohad::initSynchronizer() {
  synchronizer = std::make_shared<SynchronizerImpl>(
      consensus_gate, chain_validator, storage, block_loader);

  log_->info("[Init] => synchronizer");
}

/**
 * Initializing peer communication service
 */
void Irohad::initPeerCommunicationService() {
  pcs = std::make_shared<PeerCommunicationServiceImpl>(
      ordering_gate, synchronizer, simulator);

  pcs->on_proposal().subscribe(
      [this](auto) { log_->info("~~~~~~~~~| PROPOSAL ^_^ |~~~~~~~~~ "); });

  pcs->on_commit().subscribe(
      [this](auto) { log_->info("~~~~~~~~~| COMMIT =^._.^= |~~~~~~~~~ "); });

  // complete initialization of ordering gate
  ordering_gate->setPcs(*pcs);

  log_->info("[Init] => pcs");
}

void Irohad::initMstProcessor() {
  if (is_mst_supported_) {
    auto mst_transport = std::make_shared<MstTransportGrpc>();
    auto mst_completer = std::make_shared<DefaultCompleter>();
    auto mst_storage = std::make_shared<MstStorageStateImpl>(mst_completer);
    // TODO: IR-1317 @l4l (02/05/18) magics should be replaced with options via
    // cli parameters
    auto mst_propagation = std::make_shared<GossipPropagationStrategy>(
        std::make_shared<ametsuchi::PeerQueryWsv>(storage->getWsvQuery()),
        std::chrono::seconds(5) /*emitting period*/,
        2 /*amount per once*/);
    auto mst_time = std::make_shared<MstTimeProviderImpl>();
    mst_processor = std::make_shared<FairMstProcessor>(
        mst_transport, mst_storage, mst_propagation, mst_time);
  } else {
    mst_processor = std::make_shared<MstProcessorStub>();
  }
  log_->info("[Init] => MST processor");
}

/**
 * Initializing transaction command service
 */
void Irohad::initTransactionCommandService() {
  auto tx_processor =
      std::make_shared<TransactionProcessorImpl>(pcs, mst_processor);

  command_service = std::make_shared<::torii::CommandService>(
      tx_processor, storage, std::chrono::seconds(1), 2 * proposal_delay_);

  log_->info("[Init] => command service");
}

/**
 * Initializing query command service
 */
void Irohad::initQueryService() {
  auto query_processor = std::make_shared<QueryProcessorImpl>(storage);

  query_service = std::make_shared<::torii::QueryService>(query_processor);

  log_->info("[Init] => query service");
}

void Irohad::initWsvRestorer() {
  wsv_restorer_ = std::make_shared<iroha::ametsuchi::WsvRestorerImpl>();
}

/**
 * Run iroha daemon
 */
void Irohad::run() {
  using iroha::expected::operator|;

  // Initializing torii server
  std::string ip = "0.0.0.0";
  torii_server =
      std::make_unique<ServerRunner>(ip + ":" + std::to_string(torii_port_));

  // Initializing internal server
  internal_server =
      std::make_unique<ServerRunner>(ip + ":" + std::to_string(internal_port_));

  // Run torii server
  (torii_server->append(command_service).append(query_service).run() |
   [&](const auto &port) {
     log_->info("Torii server bound on port {}", port);
     // Run internal server
     return internal_server->append(ordering_init.ordering_gate_transport)
         .append(ordering_init.ordering_service_transport)
         .append(yac_init.consensus_network)
         .append(loader_init.service)
         .run();
   })
      .match(
          [&](const auto &port) {
            log_->info("Internal server bound on port {}", port.value);
            log_->info("===> iroha initialized");
          },
          [&](const expected::Error<std::string> &e) { log_->error(e.error); });
}
