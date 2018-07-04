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

#ifndef IROHA_SHARED_MODEL_TYPES_HPP
#define IROHA_SHARED_MODEL_TYPES_HPP

#include <cstdint>
#include <set>
#include <string>
#include <vector>

#include <boost/range/any_range.hpp>
#include "cryptography/hash.hpp"
#include "cryptography/public_key.hpp"

namespace shared_model {

  namespace interface {

    class Signature;
    class Transaction;
    class AccountAsset;

    namespace types {
      /// Type of hash
      using HashType = crypto::Hash;
      /// Blob type
      using BlobType = crypto::Blob;
      /// Type of account id
      using AccountIdType = std::string;
      /// Type of precision
      using PrecisionType = uint8_t;
      /// Type of height (for Block, Proposal etc)
      using HeightType = uint64_t;
      /// Type of peer address
      using AddressType = std::string;
      /// Type of public key
      using PubkeyType = crypto::PublicKey;
      /// Type of public keys' collection
      using PublicKeyCollectionType = std::vector<PubkeyType>;
      /// Type of role (i.e admin, user)
      using RoleIdType = std::string;
      /// Iroha domain id type
      using DomainIdType = std::string;
      /// Type of asset id
      using AssetIdType = std::string;
      /// Permission type used in permission commands
      using PermissionNameType = std::string;
      /// Permission set
      using PermissionSetType = std::set<PermissionNameType>;
      /// Type of Quorum used in transaction and set quorum
      using QuorumType = uint16_t;
      /// Type of signature range, which returns when signatures are invoked
      using SignatureRangeType = boost::any_range<const interface::Signature &,
                                                  boost::forward_traversal_tag>;
      /// Type of timestamp
      using TimestampType = uint64_t;
      /// Type of peer address
      using AddressType = std::string;
      /// Type of counter
      using CounterType = uint64_t;
      /// Type of account name
      using AccountNameType = std::string;
      /// Type of asset name
      using AssetNameType = std::string;
      /// Type of detail
      using DetailType = std::string;
      /// Type of JSON data
      using JsonType = std::string;
      /// Type of account detail key
      using AccountDetailKeyType = std::string;
      /// Type of account detail value
      using AccountDetailValueType = std::string;
      /// Type of a number of transactions in block
      using TransactionsNumberType = uint16_t;
      /// Type of transactions' collection
      using TransactionsCollectionType =
          boost::any_range<Transaction,
                           boost::random_access_traversal_tag,
                           const Transaction &>;
      using AccountAssetCollectionType =
          boost::any_range<AccountAsset,
                           boost::random_access_traversal_tag,
                           const AccountAsset &>;
      /// Type of the transfer message
      using DescriptionType = std::string;

      enum class BatchType { ATOMIC = 0, ORDERED = 1};
    }  // namespace types
  }    // namespace interface
}  // namespace shared_model
#endif  // IROHA_SHARED_MODEL_TYPES_HPP
