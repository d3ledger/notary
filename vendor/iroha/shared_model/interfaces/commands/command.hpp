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

#ifndef IROHA_SHARED_MODEL_COMMAND_HPP
#define IROHA_SHARED_MODEL_COMMAND_HPP

#include <boost/variant.hpp>
#include <utility>

#include "interfaces/base/model_primitive.hpp"
#include "interfaces/commands/add_asset_quantity.hpp"
#include "interfaces/commands/add_peer.hpp"
#include "interfaces/commands/add_signatory.hpp"
#include "interfaces/commands/append_role.hpp"
#include "interfaces/commands/create_account.hpp"
#include "interfaces/commands/create_asset.hpp"
#include "interfaces/commands/create_domain.hpp"
#include "interfaces/commands/create_role.hpp"
#include "interfaces/commands/detach_role.hpp"
#include "interfaces/commands/grant_permission.hpp"
#include "interfaces/commands/remove_signatory.hpp"
#include "interfaces/commands/revoke_permission.hpp"
#include "interfaces/commands/set_account_detail.hpp"
#include "interfaces/commands/set_quorum.hpp"
#include "interfaces/commands/subtract_asset_quantity.hpp"
#include "interfaces/commands/transfer_asset.hpp"
#include "utils/visitor_apply_for_all.hpp"

namespace shared_model {
  namespace interface {

    /**
     * Class provides commands container for all commands in system.
     * General note: this class is container for commands, not a base class.
     */
    class Command : public ModelPrimitive<Command> {
     private:
      /// const reference shortcut type
      template <typename... Value>
      using wrap = boost::variant<const Value &...>;

     public:
      /// Type of variant, that handle concrete command
      using CommandVariantType = wrap<AddAssetQuantity,
                                      AddPeer,
                                      AddSignatory,
                                      AppendRole,
                                      CreateAccount,
                                      CreateAsset,
                                      CreateDomain,
                                      CreateRole,
                                      DetachRole,
                                      GrantPermission,
                                      RemoveSignatory,
                                      RevokePermission,
                                      SetAccountDetail,
                                      SetQuorum,
                                      SubtractAssetQuantity,
                                      TransferAsset>;

      /// Types of concrete commands, in attached variant
      using CommandListType = CommandVariantType::types;

      /**
       * @return reference to const variant with concrete command
       */
      virtual const CommandVariantType &get() const = 0;

      // ------------------------| Primitive override |-------------------------

      std::string toString() const override;

      bool operator==(const ModelType &rhs) const override;
    };

  }  // namespace interface
}  // namespace shared_model
#endif  // IROHA_SHARED_MODEL_COMMAND_HPP
