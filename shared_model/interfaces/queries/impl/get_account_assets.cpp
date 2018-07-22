/**
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

#include "interfaces/queries/get_account_assets.hpp"

namespace shared_model {
  namespace interface {

    std::string GetAccountAssets::toString() const {
      return detail::PrettyStringBuilder()
          .init("GetAccountAssets")
          .append("account_id", accountId())
          .finalize();
    }

    // TODO 07/06/2018 Akvinikym: types of rhs.accountId() and rhs.assetId() should be different IR-1397
    bool GetAccountAssets::operator==(const ModelType &rhs) const {
      return accountId() == rhs.accountId();
    }

  }  // namespace interface
}  // namespace shared_model
