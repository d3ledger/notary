/**
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

#include "interfaces/commands/subtract_asset_quantity.hpp"

namespace shared_model {
  namespace interface {

    std::string SubtractAssetQuantity::toString() const {
      return detail::PrettyStringBuilder()
          .init("SubtractAssetQuantity")
          .append("asset_id", assetId())
          .append("amount", amount().toString())
          .finalize();
    }

    bool SubtractAssetQuantity::operator==(const ModelType &rhs) const {
      return assetId() == rhs.assetId() and amount() == rhs.amount();
    }

  }  // namespace interface
}  // namespace shared_model
