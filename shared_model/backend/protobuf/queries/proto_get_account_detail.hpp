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

#ifndef IROHA_PROTO_GET_ACCOUNT_DETAIL_HPP
#define IROHA_PROTO_GET_ACCOUNT_DETAIL_HPP

#include "backend/protobuf/common_objects/trivial_proto.hpp"
#include "interfaces/queries/get_account_detail.hpp"
#include "queries.pb.h"

namespace shared_model {
  namespace proto {
    class GetAccountDetail final
        : public CopyableProto<interface::GetAccountDetail,
                               iroha::protocol::Query,
                               GetAccountDetail> {
     public:
      template <typename QueryType>
      explicit GetAccountDetail(QueryType &&query);

      GetAccountDetail(const GetAccountDetail &o);

      GetAccountDetail(GetAccountDetail &&o) noexcept;

      const interface::types::AccountIdType &accountId() const override;

      boost::optional<interface::types::AccountDetailKeyType> key() const override;

      boost::optional<interface::types::AccountIdType> writer() const override;

     private:
      // ------------------------------| fields |-------------------------------

      const iroha::protocol::GetAccountDetail &account_detail_;
    };
  }  // namespace proto
}  // namespace shared_model

#endif  // IROHA_PROTO_GET_ACCOUNT_DETAIL_HPP
