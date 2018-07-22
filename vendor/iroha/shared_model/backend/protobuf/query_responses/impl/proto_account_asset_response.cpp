/**
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

#include "backend/protobuf/query_responses/proto_account_asset_response.hpp"

namespace shared_model {
  namespace proto {

    template <typename QueryResponseType>
    AccountAssetResponse::AccountAssetResponse(
        QueryResponseType &&queryResponse)
        : CopyableProto(std::forward<QueryResponseType>(queryResponse)),
          accountAssetResponse_{proto_->account_assets_response()},
          accountAssets_{[this] {
            return std::vector<proto::AccountAsset>(
                accountAssetResponse_.account_assets().begin(),
                accountAssetResponse_.account_assets().end());
          }} {}

    template AccountAssetResponse::AccountAssetResponse(
        AccountAssetResponse::TransportType &);
    template AccountAssetResponse::AccountAssetResponse(
        const AccountAssetResponse::TransportType &);
    template AccountAssetResponse::AccountAssetResponse(
        AccountAssetResponse::TransportType &&);

    AccountAssetResponse::AccountAssetResponse(const AccountAssetResponse &o)
        : AccountAssetResponse(o.proto_) {}

    AccountAssetResponse::AccountAssetResponse(AccountAssetResponse &&o)
        : AccountAssetResponse(std::move(o.proto_)) {}

    const interface::types::AccountAssetCollectionType
    AccountAssetResponse::accountAssets() const {
      return *accountAssets_;
    }

  }  // namespace proto
}  // namespace shared_model
