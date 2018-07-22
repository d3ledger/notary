/**
 * Copyright Soramitsu Co., Ltd. 2018 All Rights Reserved.
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

#ifndef TORII_COMMAND_SERVICE_HPP
#define TORII_COMMAND_SERVICE_HPP

#include <iostream>
#include <string>
#include <unordered_map>

#include "ametsuchi/storage.hpp"
#include "cache/cache.hpp"
#include "cryptography/hash.hpp"
#include "endpoint.grpc.pb.h"
#include "endpoint.pb.h"
#include "logger/logger.hpp"
#include "torii/processor/transaction_processor.hpp"

namespace torii {
  /**
   * Actual implementation of sync CommandService.
   */
  class CommandService : public iroha::protocol::CommandService::Service {
   public:
    /**
     * Creates a new instance of CommandService
     * @param tx_processor - processor of received transactions
     * @param storage - to query transactions outside the cache
     * @param initial_timeout - streaming timeout when tx is not received
     * @param nonfinal_timeout - streaming timeout when tx is being processed
     */
    CommandService(
        std::shared_ptr<iroha::torii::TransactionProcessor> tx_processor,
        std::shared_ptr<iroha::ametsuchi::Storage> storage,
        std::chrono::milliseconds initial_timeout,
        std::chrono::milliseconds nonfinal_timeout);

    /**
     * Disable copying in any way to prevent potential issues with common
     * storage/tx_processor
     */
    CommandService(const CommandService &) = delete;
    CommandService &operator=(const CommandService &) = delete;

    /**
     * Actual implementation of sync Torii in CommandService
     * @param tx - Transaction we've received
     */
    void Torii(const iroha::protocol::Transaction &tx);

    /**
     * Actual implementation of sync Torii in CommandService
     * @param tx_lis - transactions we've received
     */
    void ListTorii(const iroha::protocol::TxList &tx_list);

    /**
     * Torii call via grpc
     * @param context - call context (see grpc docs for details)
     * @param request - transaction received
     * @param response - no actual response (grpc stub for empty answer)
     * @return - grpc::Status
     */
    virtual grpc::Status Torii(grpc::ServerContext *context,
                               const iroha::protocol::Transaction *request,
                               google::protobuf::Empty *response) override;

    /**
     * Torii call for transactions list via grpc
     * @param context - call context (see grpc docs for details)
     * @param request - list of transactions received
     * @param response - no actual response (grpc stub for empty answer)
     * @return - grpc::Status
     */
    virtual grpc::Status ListTorii(grpc::ServerContext *context,
                                   const iroha::protocol::TxList *request,
                                   google::protobuf::Empty *response) override;

    /**
     * Request to retrieve a status of any particular transaction
     * @param request - TxStatusRequest object which identifies transaction
     * uniquely
     * @param response - ToriiResponse which contains a current state of
     * requested transaction
     */
    void Status(const iroha::protocol::TxStatusRequest &request,
                iroha::protocol::ToriiResponse &response);

    /**
     * Status call via grpc
     * @param context - call context
     * @param request - TxStatusRequest object which identifies transaction
     * uniquely
     * @param response - ToriiResponse which contains a current state of
     * requested transaction
     * @return - grpc::Status
     */
    virtual grpc::Status Status(
        grpc::ServerContext *context,
        const iroha::protocol::TxStatusRequest *request,
        iroha::protocol::ToriiResponse *response) override;

    /**
     * Streaming call which will repeatedly send all statuses of requested
     * transaction from its status at the moment of receiving this request to
     * the some final transaction status (which cannot change anymore)
     * @param request- TxStatusRequest object which identifies transaction
     * uniquely
     * @return observable with transaction statuses
     */
    rxcpp::observable<
        std::shared_ptr<shared_model::interface::TransactionResponse>>
    StatusStream(const shared_model::crypto::Hash &hash);

    /**
     * StatusStream call via grpc
     * @param context - call context
     * @param request - TxStatusRequest object which identifies transaction
     * uniquely
     * @param response_writer - grpc::ServerWriter which can repeatedly send
     * transaction statuses back to the client
     * @return - grpc::Status
     */
    grpc::Status StatusStream(grpc::ServerContext *context,
                              const iroha::protocol::TxStatusRequest *request,
                              grpc::ServerWriter<iroha::protocol::ToriiResponse>
                                  *response_writer) override;

   private:
    /**
     * Execute events scheduled in run loop until it is not empty and the
     * subscriber is active
     * @param subscription - tx status subscription
     * @param run_loop - gRPC thread run loop
     */
    inline void handleEvents(rxcpp::composite_subscription &subscription,
                             rxcpp::schedulers::run_loop &run_loop);

    void addTxToCacheAndLog(const std::string &who,
                            const shared_model::crypto::Hash &hash,
                            const iroha::protocol::ToriiResponse &response);

   private:
    using CacheType = iroha::cache::Cache<shared_model::crypto::Hash,
                                          iroha::protocol::ToriiResponse,
                                          shared_model::crypto::Hash::Hasher>;

    std::shared_ptr<iroha::torii::TransactionProcessor> tx_processor_;
    std::shared_ptr<iroha::ametsuchi::Storage> storage_;
    std::chrono::milliseconds initial_timeout_;
    std::chrono::milliseconds nonfinal_timeout_;
    std::shared_ptr<CacheType> cache_;

    /**
     * Mutex for propagating stateless validation status
     */
    std::mutex stateless_tx_status_notifier_mutex_;

    /**
     * Subject with stateless validation statuses
     */
    rxcpp::subjects::subject<
        std::shared_ptr<shared_model::interface::TransactionResponse>>
        stateless_notifier_;

    /**
     * Observable with all transaction statuses
     */
    rxcpp::observable<
        std::shared_ptr<shared_model::interface::TransactionResponse>>
        responses_;

    logger::Logger log_;
  };

}  // namespace torii

#endif  // TORII_COMMAND_SERVICE_HPP
