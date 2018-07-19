/**
 * Copyright Soramitsu Co., Ltd. 2018 All Rights Reserved.
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

#ifndef IROHA_COMMON_EXECUTOR_HPP
#define IROHA_COMMON_EXECUTOR_HPP

#include <set>

#include "ametsuchi/wsv_query.hpp"
#include "interfaces/permissions.hpp"

namespace iroha {

  /**
   * Check that account has role permission
   * @param account_id - account to check
   * @param queries - WsvQueries
   * @param permission_id  = permission to check
   * @return  True if account has permission, false otherwise
   */
  bool checkAccountRolePermission(
      const shared_model::interface::types::AccountIdType &account_id,
      iroha::ametsuchi::WsvQuery &queries,
      shared_model::interface::permissions::Role permission_id);

  /**
   * Accumulate all account's role permissions
   * @param account_id
   * @param queries - WSVqueries
   * @return set of account's role permissions
   */
  boost::optional<shared_model::interface::RolePermissionSet>
  getAccountPermissions(
      const shared_model::interface::types::AccountIdType &account_id,
      iroha::ametsuchi::WsvQuery &queries);

}  // namespace iroha

#endif  // IROHA_COMMON_EXECUTOR_HPP
