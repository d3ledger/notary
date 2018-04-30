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

#ifndef IROHA_SHARED_MODEL_ADD_PEER_HPP
#define IROHA_SHARED_MODEL_ADD_PEER_HPP

#include "interfaces/base/primitive.hpp"
#include "interfaces/common_objects/peer.hpp"
#include "interfaces/common_objects/types.hpp"

#ifndef DISABLE_BACKWARD
#include "model/commands/add_peer.hpp"
#endif

namespace shared_model {
  namespace interface {

    /**
     * Add new peer to Iroha
     */
    class AddPeer : public PRIMITIVE(AddPeer) {
     public:
      /**
       * Return peer to be added by the command.
       * @return Peer
       */
      virtual const interface::Peer &peer() const = 0;

      std::string toString() const override {
        return detail::PrettyStringBuilder()
            .init("AddPeer")
            .append("peer_address", peer().address())
            .append("pubkey", peer().pubkey().toString())
            .finalize();
      }

#ifndef DISABLE_BACKWARD
      OldModelType *makeOldModel() const override {
        auto oldModel = new iroha::model::AddPeer;
        oldModel->peer.address = peer().address();
        oldModel->peer.pubkey =
            peer().pubkey().makeOldModel<decltype(oldModel->peer.pubkey)>();
        return oldModel;
      }
#endif

      bool operator==(const ModelType &rhs) const override {
        return peer() == rhs.peer();
      }
    };
  }  // namespace interface
}  // namespace shared_model

#endif  // IROHA_SHARED_MODEL_ADD_PEER_HPP
