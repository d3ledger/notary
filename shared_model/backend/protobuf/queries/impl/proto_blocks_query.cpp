/**
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

#include "backend/protobuf/queries/proto_blocks_query.hpp"
#include "backend/protobuf/util.hpp"

namespace shared_model {
  namespace proto {

    template <typename BlocksQueryType>
    BlocksQuery::BlocksQuery(BlocksQueryType &&query)
        : CopyableProto(std::forward<BlocksQueryType>(query)),
          blob_{[this] { return makeBlob(*proto_); }},
          payload_{[this] { return makeBlob(proto_->meta()); }},
          signatures_{[this] {
            SignatureSetType<proto::Signature> set;
            if (proto_->has_signature()) {
              set.emplace(proto_->signature());
            }
            return set;
          }} {}

    template BlocksQuery::BlocksQuery(BlocksQuery::TransportType &);
    template BlocksQuery::BlocksQuery(const BlocksQuery::TransportType &);
    template BlocksQuery::BlocksQuery(BlocksQuery::TransportType &&);

    BlocksQuery::BlocksQuery(const BlocksQuery &o) : BlocksQuery(o.proto_) {}

    BlocksQuery::BlocksQuery(BlocksQuery &&o) noexcept
        : BlocksQuery(std::move(o.proto_)) {}

    const interface::types::AccountIdType &BlocksQuery::creatorAccountId()
        const {
      return proto_->meta().creator_account_id();
    }

    interface::types::CounterType BlocksQuery::queryCounter() const {
      return proto_->meta().query_counter();
    }

    const interface::types::BlobType &BlocksQuery::blob() const {
      return *blob_;
    }

    const interface::types::BlobType &BlocksQuery::payload() const {
      return *payload_;
    }

    interface::types::SignatureRangeType BlocksQuery::signatures() const {
      return *signatures_;
    }

    bool BlocksQuery::addSignature(const crypto::Signed &signed_blob,
                                   const crypto::PublicKey &public_key) {
      if (proto_->has_signature()) {
        return false;
      }

      auto sig = proto_->mutable_signature();
      sig->set_signature(crypto::toBinaryString(signed_blob));
      sig->set_pubkey(crypto::toBinaryString(public_key));
      return true;
    }

    interface::types::TimestampType BlocksQuery::createdTime() const {
      return proto_->meta().created_time();
    }

  }  // namespace proto
}  // namespace shared_model
