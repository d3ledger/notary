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

#ifndef IROHA_SHARED_MODEL_TRANSFER_ASSET_HPP
#define IROHA_SHARED_MODEL_TRANSFER_ASSET_HPP

#include "interfaces/base/model_primitive.hpp"
#include "interfaces/common_objects/amount.hpp"
#include "interfaces/common_objects/types.hpp"

namespace shared_model {
  namespace interface {
    /**
     * Grant permission to account
     */
    class TransferAsset : public ModelPrimitive<TransferAsset> {
     public:
      /**
       * @return Id of the account from which transfer assets
       */
      virtual const types::AccountIdType &srcAccountId() const = 0;
      /**
       * @return Id of the account to which transfer assets
       */
      virtual const types::AccountIdType &destAccountId() const = 0;
      /**
       * @return Id of the asset to transfer
       */
      virtual const types::AssetIdType &assetId() const = 0;
      /**
       * @return asset amount to transfer
       */
      virtual const Amount &amount() const = 0;
      /**
       * @return message of the transfer
       */
      virtual const types::DescriptionType &description() const = 0;

      std::string toString() const override;

      bool operator==(const ModelType &rhs) const override;
    };
  }  // namespace interface
}  // namespace shared_model

#endif  // IROHA_SHARED_MODEL_TRANSFER_ASSET_HPP
