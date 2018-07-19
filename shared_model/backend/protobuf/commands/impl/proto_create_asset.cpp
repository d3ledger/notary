/**
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

#include "backend/protobuf/commands/proto_create_asset.hpp"

namespace shared_model {
  namespace proto {

    template <typename CommandType>
    CreateAsset::CreateAsset(CommandType &&command)
        : CopyableProto(std::forward<CommandType>(command)),
          create_asset_{proto_->create_asset()},
          precision_{[this] { return create_asset_.precision(); }} {}

    template CreateAsset::CreateAsset(CreateAsset::TransportType &);
    template CreateAsset::CreateAsset(const CreateAsset::TransportType &);
    template CreateAsset::CreateAsset(CreateAsset::TransportType &&);

    CreateAsset::CreateAsset(const CreateAsset &o) : CreateAsset(o.proto_) {}

    CreateAsset::CreateAsset(CreateAsset &&o) noexcept
        : CreateAsset(std::move(o.proto_)) {}

    const interface::types::AssetNameType &CreateAsset::assetName() const {
      return create_asset_.asset_name();
    }

    const interface::types::DomainIdType &CreateAsset::domainId() const {
      return create_asset_.domain_id();
    }

    const CreateAsset::PrecisionType &CreateAsset::precision() const {
      return *precision_;
    }

  }  // namespace proto
}  // namespace shared_model
