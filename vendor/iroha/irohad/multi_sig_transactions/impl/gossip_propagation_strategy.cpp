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

#include "multi_sig_transactions/gossip_propagation_strategy.hpp"

#include <numeric>
#include <random>

#include <boost/assert.hpp>
#include <boost/range/irange.hpp>
#include "common/types.hpp"

namespace iroha {

  using PropagationData = PropagationStrategy::PropagationData;
  using OptPeer = GossipPropagationStrategy::OptPeer;
  using PeerProvider = GossipPropagationStrategy::PeerProvider;
  using std::chrono::steady_clock;

  GossipPropagationStrategy::GossipPropagationStrategy(
      PeerProvider query, std::chrono::milliseconds period, uint32_t amount)
      : query(query),
        non_visited({}),
        emitent(rxcpp::observable<>::interval(steady_clock::now(), period)
                    .map([this, amount](int) {
                      PropagationData vec;
                      auto range = boost::irange(0u, amount);
                      // push until find empty element
                      std::find_if_not(
                          range.begin(), range.end(), [this, &vec](auto) {
                            return this->visit() | [&vec](auto e) -> bool {
                              vec.push_back(e);
                              return true;  // proceed
                            };
                          });
                      return vec;
                    })) {}

  rxcpp::observable<PropagationData> GossipPropagationStrategy::emitter() {
    return emitent.subscribe_on(rxcpp::observe_on_new_thread());
  }

  GossipPropagationStrategy::~GossipPropagationStrategy() {
    // Make sure that emitent callback have finish and haven't started yet
    std::lock_guard<std::mutex> lock(m);
    query.reset();
  }

  bool GossipPropagationStrategy::initQueue() {
    return query->getLedgerPeers() |
               [](auto &&data) -> boost::optional<PropagationData> {
      if (data.size() == 0) {
        return {};
      }
      return std::move(data);
    } | [this](auto &&data) -> bool {  // nullopt implicitly casts to false
      this->last_data = std::move(data);
      this->non_visited.resize(this->last_data.size());
      std::iota(this->non_visited.begin(), this->non_visited.end(), 0);
      std::shuffle(this->non_visited.begin(),
                   this->non_visited.end(),
                   std::default_random_engine{});
      return true;
    };
  }

  OptPeer GossipPropagationStrategy::visit() {
    // Make sure that dtor isn't running
    std::lock_guard<std::mutex> lock(m);
    if (not query or (non_visited.empty() and not initQueue())) {
      // either PeerProvider doesn't gives peers / dtor have been called
      return {};
    }
    // or non_visited non-empty
    BOOST_ASSERT(not non_visited.empty());
    BOOST_ASSERT(non_visited.back() < last_data.size());

    auto el = last_data[non_visited.back()];
    non_visited.pop_back();
    return el;
  }

}  // namespace iroha
