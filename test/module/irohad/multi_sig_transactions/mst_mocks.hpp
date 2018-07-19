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

#ifndef IROHA_MST_MOCKS_HPP
#define IROHA_MST_MOCKS_HPP

#include <gmock/gmock.h>
#include "multi_sig_transactions/mst_processor.hpp"
#include "multi_sig_transactions/mst_propagation_strategy.hpp"
#include "multi_sig_transactions/mst_time_provider.hpp"
#include "multi_sig_transactions/mst_types.hpp"
#include "network/mst_transport.hpp"

namespace iroha {

  class MockMstTransport : public network::MstTransport {
   public:
    MOCK_METHOD1(subscribe,
                 void(std::shared_ptr<network::MstTransportNotification>));
    MOCK_METHOD2(sendState,
                 void(const shared_model::interface::Peer &to,
                      const MstState &providing_state));
  };

  /**
   * Transport notification mock
   */
  class MockMstTransportNotification
      : public network::MstTransportNotification {
   public:
    MOCK_METHOD2(
        onNewState,
        void(const std::shared_ptr<shared_model::interface::Peer> &peer,
             const MstState &state));
  };

  /**
   * Propagation strategy mock
   */
  class MockPropagationStrategy : public PropagationStrategy {
   public:
    MOCK_METHOD0(emitter, rxcpp::observable<PropagationData>());
  };

  /**
   * Time provider mock
   */
  class MockTimeProvider : public MstTimeProvider {
   public:
    MOCK_CONST_METHOD0(getCurrentTime, TimeType());
  };

  struct MockMstProcessor : public MstProcessor {
    MOCK_METHOD1(propagateTransactionImpl, void(const DataType));
    MOCK_CONST_METHOD0(onStateUpdateImpl,
                       rxcpp::observable<std::shared_ptr<MstState>>());
    MOCK_CONST_METHOD0(onPreparedTransactionsImpl,
                       rxcpp::observable<DataType>());
    MOCK_CONST_METHOD0(onExpiredTransactionsImpl,
                       rxcpp::observable<DataType>());
  };
}  // namespace iroha
#endif  // IROHA_MST_MOCKS_HPP
