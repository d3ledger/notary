/**
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef IROHA_SHARED_MODEL_BLOCKS_QUERY_HPP
#define IROHA_SHARED_MODEL_BLOCKS_QUERY_HPP

#include <boost/variant.hpp>

#include "interfaces/base/model_primitive.hpp"
#include "interfaces/base/signable.hpp"
#include "interfaces/common_objects/types.hpp"

namespace shared_model {
  namespace interface {

    /**
     * Class BlocksQuery provides container with one of concrete query available
     * in system. General note: this class is container for queries but not a
     * base class.
     */
    class BlocksQuery : public Signable<BlocksQuery> {
     public:
      /**
       * @return id of query creator
       */
      virtual const types::AccountIdType &creatorAccountId() const = 0;

      /**
       * Query counter - incremental variable reflect for number of sent to
       * system queries plus 1. Required for preventing replay attacks.
       * @return attached query counter
       */
      virtual types::CounterType queryCounter() const = 0;

      // ------------------------| Primitive override |-------------------------

      std::string toString() const override {
        return detail::PrettyStringBuilder()
            .init("BlocksQuery")
            .append("creatorId", creatorAccountId())
            .append("queryCounter", std::to_string(queryCounter()))
            .append(Signable::toString())
            .finalize();
      }

      bool operator==(const ModelType &rhs) const override {
        return creatorAccountId() == rhs.creatorAccountId()
            && queryCounter() == rhs.queryCounter()
            && createdTime() == rhs.createdTime()
            && signatures() == rhs.signatures();
      }
    };
  }  // namespace interface
}  // namespace shared_model
#endif  // IROHA_SHARED_MODEL_BLOCKS_QUERY_HPP
