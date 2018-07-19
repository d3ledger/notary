/**
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

#include "backend/protobuf/query_responses/proto_roles_response.hpp"
#include <boost/range/numeric.hpp>

namespace shared_model {
  namespace proto {

    template <typename QueryResponseType>
    RolesResponse::RolesResponse(QueryResponseType &&queryResponse)
        : CopyableProto(std::forward<QueryResponseType>(queryResponse)),
          rolesResponse_{proto_->roles_response()},
          roles_{[this] {
            return boost::accumulate(rolesResponse_.roles(),
                                     RolesIdType{},
                                     [](auto &&roles, const auto &role) {
                                       roles.emplace_back(role);
                                       return std::move(roles);
                                     });
          }} {}

    template RolesResponse::RolesResponse(RolesResponse::TransportType &);
    template RolesResponse::RolesResponse(const RolesResponse::TransportType &);
    template RolesResponse::RolesResponse(RolesResponse::TransportType &&);

    RolesResponse::RolesResponse(const RolesResponse &o)
        : RolesResponse(o.proto_) {}

    RolesResponse::RolesResponse(RolesResponse &&o)
        : RolesResponse(std::move(o.proto_)) {}

    const RolesResponse::RolesIdType &RolesResponse::roles() const {
      return *roles_;
    }

  }  // namespace proto
}  // namespace shared_model
