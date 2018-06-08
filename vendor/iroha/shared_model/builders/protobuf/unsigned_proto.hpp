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

#ifndef IROHA_UNSIGNED_PROTO_HPP
#define IROHA_UNSIGNED_PROTO_HPP

#include "backend/protobuf/common_objects/signature.hpp"
#include "cryptography/crypto_provider/crypto_signer.hpp"
#include "cryptography/keypair.hpp"
#include "interfaces/common_objects/types.hpp"

namespace shared_model {
  namespace proto {
    /**
     * Class for holding built but still unsigned objects
     * @tparam T - type of object received from builder
     */
    template <typename T>
    class UnsignedWrapper {
     public:
      using ModelType = T;

      /**
       * Constructs new unsigned object instance
       * @param o - object received from builder
       */
      explicit UnsignedWrapper(const T &o) : object_(o) {}

      explicit UnsignedWrapper(T &&o) : object_(std::move(o)) {}

      /**
       * Add signature and retrieve signed result
       * @param signature - signature to add
       * @return signed object
       */
      UnsignedWrapper &signAndAddSignature(const crypto::Keypair &keypair) {
        auto signedBlob = shared_model::crypto::CryptoSigner<>::sign(
            shared_model::crypto::Blob(object_.payload()), keypair);
        object_.addSignature(signedBlob, keypair.publicKey());
        // TODO: 05.12.2017 luckychess think about false case
        return *this;
      }

      /**
       * Finishes object building
       * @return built signed object
       */
      T finish() {
        if (boost::size(object_.signatures()) == 0) {
          throw std::invalid_argument("Cannot get object without signatures");
        }
        return object_;
      }

      interface::types::HashType hash() {
        return object_.hash();
      }

     private:
      T object_;
    };
  }  // namespace proto
}  // namespace shared_model

#endif  // IROHA_UNSIGNED_PROTO_HPP
