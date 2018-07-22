/**
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef IROHA_SHARED_MODEL_AMOUNT_HPP
#define IROHA_SHARED_MODEL_AMOUNT_HPP

#include "interfaces/base/model_primitive.hpp"

#include <boost/multiprecision/cpp_int.hpp>
#include "interfaces/common_objects/types.hpp"
#include "utils/lazy_initializer.hpp"

namespace shared_model {
  namespace interface {

    /**
     * Representation of fixed point number
     */
    class Amount final : public ModelPrimitive<Amount> {
     public:
      explicit Amount(const std::string &amount);
      explicit Amount(std::string &&amount);

      Amount(const Amount &o);
      Amount(Amount &&o) noexcept;

      /**
       * Gets integer representation value, which ignores precision
       * @return amount represented as integer value, which ignores precision
       */
      const boost::multiprecision::uint256_t &intValue() const;

      /**
       * Gets the position of precision
       * @return the position of precision
       */
      types::PrecisionType precision() const;

      /**
       * String representation.
       * @return string representation of the asset.
       */
      std::string toStringRepr() const;

      /**
       * Checks equality of objects inside
       * @param rhs - other wrapped value
       * @return true, if wrapped objects are same
       */
      bool operator==(const ModelType &rhs) const override;

      /**
       * Stringify the data.
       * @return the content of asset.
       */
      std::string toString() const override;

      Amount *clone() const override;

     private:
      const std::string amount_;

      interface::types::PrecisionType precision_;
      const boost::multiprecision::uint256_t multiprecision_repr_;
    };
  }  // namespace interface
}  // namespace shared_model
#endif  // IROHA_SHARED_MODEL_AMOUNT_HPP
