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

#ifndef IROHA_PROTO_ADD_SIGNATORY_HPP
#define IROHA_PROTO_ADD_SIGNATORY_HPP

#include "backend/protobuf/common_objects/trivial_proto.hpp"
#include "commands.pb.h"
#include "interfaces/commands/add_signatory.hpp"

namespace shared_model {
  namespace proto {
    class AddSignatory final : public CopyableProto<interface::AddSignatory,
                                                    iroha::protocol::Command,
                                                    AddSignatory> {
     public:
      template <typename CommandType>
      explicit AddSignatory(CommandType &&command);

      AddSignatory(const AddSignatory &o);

      AddSignatory(AddSignatory &&o) noexcept;

      const interface::types::AccountIdType &accountId() const override;

      const interface::types::PubkeyType &pubkey() const override;

     private:
      // lazy
      template <typename Value>
      using Lazy = detail::LazyInitializer<Value>;

      const iroha::protocol::AddSignatory &add_signatory_;

      const Lazy<interface::types::PubkeyType> pubkey_;
    };

  }  // namespace proto
}  // namespace shared_model

#endif  // IROHA_PROTO_ADD_SIGNATORY_HPP
