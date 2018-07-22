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

#ifndef IROHA_MST_PROPAGATION_STRATEGY_HPP
#define IROHA_MST_PROPAGATION_STRATEGY_HPP

#include <rxcpp/rx.hpp>
#include <vector>
#include "interfaces/common_objects/peer.hpp"

namespace iroha {

  /**
   * Interface provides strategy for propagation states in network
   */
  class PropagationStrategy {
   public:
    virtual ~PropagationStrategy() = default;
    using PropagationData =
        std::vector<std::shared_ptr<shared_model::interface::Peer>>;

    /**
     * Provides observable that will be emit new results
     * with respect to own strategy
     */
    virtual rxcpp::observable<PropagationData> emitter() = 0;
  };
}  // namespace iroha

#endif  // IROHA_MST_PROPAGATION_STRATEGY_HPP
