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

#ifndef IROHA_SHARED_MODEL_CREATE_ASSET_HPP
#define IROHA_SHARED_MODEL_CREATE_ASSET_HPP

#include "interfaces/base/primitive.hpp"
#include "interfaces/common_objects/types.hpp"

#ifndef DISABLE_BACKWARD
#include "model/commands/create_asset.hpp"
#endif

namespace shared_model {
  namespace interface {
    /**
     * Create asset in Iroha domain
     */
    class CreateAsset : public PRIMITIVE(CreateAsset) {
     public:
      /**
       * @return Asset name to create
       */
      virtual const types::AssetNameType &assetName() const = 0;
      /**
       * @return Iroha domain of the asset
       */
      virtual const types::DomainIdType &domainId() const = 0;
      /// Precision type
      using PrecisionType = uint8_t;
      /**
       * @return precision of the asset
       */
      virtual const PrecisionType &precision() const = 0;

      std::string toString() const override {
        return detail::PrettyStringBuilder()
            .init("CreateAsset")
            .append("asset_name", assetName())
            .append("domain_id", domainId())
            .append("precision", std::to_string(precision()))
            .finalize();
      }

#ifndef DISABLE_BACKWARD
      OldModelType *makeOldModel() const override {
        auto oldModel = new iroha::model::CreateAsset;
        oldModel->asset_name = assetName();
        oldModel->domain_id = domainId();
        oldModel->precision = precision();
        return oldModel;
      }

#endif

      bool operator==(const ModelType &rhs) const override {
        return assetName() == rhs.assetName() and domainId() == rhs.domainId()
            and precision() == rhs.precision();
      }
    };
  }  // namespace interface
}  // namespace shared_model

#endif  // IROHA_SHARED_MODEL_CREATE_ASSET_HPP
