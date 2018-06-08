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

#ifndef IROHA_PROTO_GET_ASSET_INFO_H
#define IROHA_PROTO_GET_ASSET_INFO_H

#include "interfaces/queries/get_asset_info.hpp"

#include "queries.pb.h"
#include "utils/lazy_initializer.hpp"

namespace shared_model {
  namespace proto {
    class GetAssetInfo final : public CopyableProto<interface::GetAssetInfo,
                                                    iroha::protocol::Query,
                                                    GetAssetInfo> {
     public:
      template <typename QueryType>
      explicit GetAssetInfo(QueryType &&query)
          : CopyableProto(std::forward<QueryType>(query)) {}

      GetAssetInfo(const GetAssetInfo &o) : GetAssetInfo(o.proto_) {}

      GetAssetInfo(GetAssetInfo &&o) noexcept
          : GetAssetInfo(std::move(o.proto_)) {}

      const interface::types::AssetIdType &assetId() const override {
        return asset_info_.asset_id();
      }

     private:
      // ------------------------------| fields |-------------------------------
      const iroha::protocol::GetAssetInfo &asset_info_{
          proto_->payload().get_asset_info()};
    };

  }  // namespace proto
}  // namespace shared_model

#endif  // IROHA_PROTO_GET_ASSET_INFO_H
