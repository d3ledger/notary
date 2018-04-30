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
#ifndef IROHA_DOMAIN_HPP
#define IROHA_DOMAIN_HPP

#include <common/types.hpp>
#include <string>

namespace iroha {
  namespace model {

    /**
     * Domain Model
     */
    struct Domain {
      /**
       * Domain unique identifier (full name)
       */
      std::string domain_id;

      /**
       * Default role for users in this domain
       */
      std::string default_role;
    };
  }  // namespace model
}  // namespace iroha

#endif  // IROHA_DOMAIN_HPP
