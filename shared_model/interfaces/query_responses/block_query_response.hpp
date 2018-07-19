/**
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef IROHA_SHARED_MODEL_BLOCK_QUERY_RESPONSE_HPP
#define IROHA_SHARED_MODEL_BLOCK_QUERY_RESPONSE_HPP

#include <boost/variant.hpp>
#include "interfaces/base/model_primitive.hpp"
#include "interfaces/query_responses/block_error_response.hpp"
#include "interfaces/query_responses/block_response.hpp"

namespace shared_model {
  namespace interface {
    /**
     * Class BlockQueryResponse(qr) provides container with concrete block query
     * responses available in the system. General note: this class is container
     * for QRs but not a base class.
     */
    class BlockQueryResponse : public ModelPrimitive<BlockQueryResponse> {
     private:
      /// Shortcut type for polymorphic wrapper
      template <typename... Value>
      using w = boost::variant<const Value &...>;

     public:
      /// Type of container with all concrete query response
      using QueryResponseVariantType = w<BlockResponse, BlockErrorResponse>;

      /// Type of all available query responses
      using QueryResponseListType = QueryResponseVariantType::types;

      /**
       * @return reference to const variant with concrete qr
       */
      virtual const QueryResponseVariantType &get() const = 0;

      // ------------------------| Primitive override |-------------------------

      std::string toString() const override;

      bool operator==(const ModelType &rhs) const override;
    };
  }  // namespace interface
}  // namespace shared_model
#endif  // IROHA_SHARED_MODEL_BLOCK_QUERY_RESPONSE_HPP
