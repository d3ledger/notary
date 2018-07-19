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

#ifndef IROHA_PROTO_ADD_PEER_HPP
#define IROHA_PROTO_ADD_PEER_HPP

#include "backend/protobuf/common_objects/peer.hpp"
#include "commands.pb.h"
#include "interfaces/commands/add_peer.hpp"
#include "interfaces/common_objects/peer.hpp"

namespace shared_model {
  namespace proto {

    class AddPeer final : public CopyableProto<interface::AddPeer,
                                               iroha::protocol::Command,
                                               AddPeer> {
     public:
      template <typename CommandType>
      explicit AddPeer(CommandType &&command);

      AddPeer(const AddPeer &o);

      AddPeer(AddPeer &&o) noexcept;

      const interface::Peer &peer() const override;

     private:
      // lazy
      template <typename Value>
      using Lazy = detail::LazyInitializer<Value>;

      const iroha::protocol::AddPeer &add_peer_;
      const Lazy<proto::Peer> peer_;
    };
  }  // namespace proto
}  // namespace shared_model

#endif  // IROHA_PROTO_ADD_PEER_HPP
