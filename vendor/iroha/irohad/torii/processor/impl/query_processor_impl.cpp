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
#include "validation/utils.hpp"

namespace iroha {
  namespace torii {
    /**
     * Builds QueryResponse that contains StatefulError
     * @param hash - original query hash
     * @return QueryRepsonse
     */
    std::shared_ptr<shared_model::interface::QueryResponse> buildStatefulError(
        const shared_model::interface::types::HashType &hash) {
      return clone(
          shared_model::proto::TemplateQueryResponseBuilder<>()
              .queryHash(hash)
              .errorQueryResponse<
                  shared_model::interface::StatefulFailedErrorResponse>()
              .build());
    }

    QueryProcessorImpl::QueryProcessorImpl(
        std::shared_ptr<ametsuchi::Storage> storage)
        : storage_(storage) {}

    bool QueryProcessorImpl::checkSignatories(
        const shared_model::interface::Query &qry) {
      const auto &wsv_query = storage_->getWsvQuery();

      auto signatories = wsv_query->getSignatories(qry.creatorAccountId());
      const auto &sig = qry.signatures();

      return boost::size(sig) == 1
          and signatories | [&sig](const auto &signatories) {
                return validation::signaturesSubset(sig, signatories);
              };
    }

    void QueryProcessorImpl::queryHandle(
        std::shared_ptr<shared_model::interface::Query> qry) {
      if (not checkSignatories(*qry)) {
        auto response = buildStatefulError(qry->hash());
        subject_.get_subscriber().on_next(response);
        return;
      }

      const auto &wsv_query = storage_->getWsvQuery();
      auto qpf = QueryProcessingFactory(wsv_query, storage_->getBlockQuery());
      auto qpf_response = qpf.validateAndExecute(*qry);
      auto qry_resp =
          std::static_pointer_cast<shared_model::proto::QueryResponse>(
              qpf_response);
      std::lock_guard<std::mutex> lock(notifier_mutex_);
      subject_.get_subscriber().on_next(
          std::make_shared<shared_model::proto::QueryResponse>(
              qry_resp->getTransport()));
    }
    rxcpp::observable<std::shared_ptr<shared_model::interface::QueryResponse>>
    QueryProcessorImpl::queryNotifier() {
      return subject_.get_observable();
    }

  }  // namespace torii
}  // namespace iroha
