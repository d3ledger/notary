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

#include "torii/processor/query_processor_impl.hpp"
#include "boost/range/size.hpp"
#include "builders/protobuf/query_responses/proto_block_query_response_builder.hpp"
#include "validation/utils.hpp"

namespace iroha {
  namespace torii {
    /**
     * Builds QueryResponse that contains StatefulError
     * @param hash - original query hash
     * @return QueryRepsonse
     */
    auto buildStatefulError(
        const shared_model::interface::types::HashType &hash) {
      return clone(
          shared_model::proto::TemplateQueryResponseBuilder<>()
              .queryHash(hash)
              .errorQueryResponse<
                  shared_model::interface::StatefulFailedErrorResponse>()
              .build());
    }
    std::shared_ptr<shared_model::interface::BlockQueryResponse>
    buildBlocksQueryError(const std::string &message) {
      return clone(shared_model::proto::BlockQueryResponseBuilder()
                       .errorResponse(message)
                       .build());
    }

    std::shared_ptr<shared_model::interface::BlockQueryResponse>
    buildBlocksQueryBlock(shared_model::interface::Block &block) {
      return clone(shared_model::proto::BlockQueryResponseBuilder()
                       .blockResponse(block)
                       .build());
    }

    QueryProcessorImpl::QueryProcessorImpl(
        std::shared_ptr<ametsuchi::Storage> storage)
        : storage_(storage) {
      storage_->on_commit().subscribe(
          [this](std::shared_ptr<shared_model::interface::Block> block) {
            auto response = buildBlocksQueryBlock(*block);
            blocksQuerySubject_.get_subscriber().on_next(response);
          });
    }
    template <class Q>
    bool QueryProcessorImpl::checkSignatories(const Q &qry) {
      const auto &wsv_query = storage_->getWsvQuery();

      auto signatories = wsv_query->getSignatories(qry.creatorAccountId());
      const auto &sig = qry.signatures();

      return boost::size(sig) == 1
          and signatories | [&sig](const auto &signatories) {
                return validation::signaturesSubset(sig, signatories);
              };
    }

    template bool QueryProcessorImpl::checkSignatories<
        shared_model::interface::Query>(const shared_model::interface::Query &);
    template bool
    QueryProcessorImpl::checkSignatories<shared_model::interface::BlocksQuery>(
        const shared_model::interface::BlocksQuery &);

    std::unique_ptr<shared_model::interface::QueryResponse>
    QueryProcessorImpl::queryHandle(const shared_model::interface::Query &qry) {
      if (not checkSignatories(qry)) {
        return buildStatefulError(qry.hash());
      }

      const auto &wsv_query = storage_->getWsvQuery();
      auto qpf = QueryProcessingFactory(wsv_query, storage_->getBlockQuery());
      auto qpf_response = qpf.validateAndExecute(qry);
      auto qry_resp =
          std::static_pointer_cast<shared_model::proto::QueryResponse>(
              qpf_response);
      return std::make_unique<shared_model::proto::QueryResponse>(
          qry_resp->getTransport());
    }

    rxcpp::observable<
        std::shared_ptr<shared_model::interface::BlockQueryResponse>>
    QueryProcessorImpl::blocksQueryHandle(
        const shared_model::interface::BlocksQuery &qry) {
      if (not checkSignatories(qry)) {
        auto response = buildBlocksQueryError("Wrong signatories");
        return rxcpp::observable<>::just(
            std::shared_ptr<shared_model::interface::BlockQueryResponse>(
                response));
      }
      const auto &wsv_query = storage_->getWsvQuery();
      auto qpf = QueryProcessingFactory(wsv_query, storage_->getBlockQuery());
      if (not qpf.validate(qry)) {
        auto response = buildBlocksQueryError("Stateful invalid");
        return rxcpp::observable<>::just(
            std::shared_ptr<shared_model::interface::BlockQueryResponse>(
                response));
      }
      return blocksQuerySubject_.get_observable();
    }

  }  // namespace torii
}  // namespace iroha
