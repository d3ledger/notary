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

#include <gtest/gtest.h>
#include <string>
#include "builders/protobuf/common_objects/proto_signature_builder.hpp"
#include "consensus/yac/impl/yac_hash_provider_impl.hpp"
#include "module/shared_model/builders/protobuf/test_block_builder.hpp"

using namespace iroha::consensus::yac;

TEST(YacHashProviderTest, MakeYacHashTest) {
  YacHashProviderImpl hash_provider;
  shared_model::interface::BlockVariant block =
      std::make_shared<shared_model::proto::Block>(TestBlockBuilder().build());

  block.addSignature(shared_model::crypto::Signed("data"),
                     shared_model::crypto::PublicKey("key"));

  auto hex_test_hash = block.hash().hex();

  auto yac_hash = hash_provider.makeHash(block);

  ASSERT_EQ(hex_test_hash, yac_hash.proposal_hash);
  ASSERT_EQ(hex_test_hash, yac_hash.block_hash);
}

TEST(YacHashProviderTest, ToModelHashTest) {
  YacHashProviderImpl hash_provider;
  shared_model::interface::BlockVariant block =
      std::make_shared<shared_model::proto::Block>(TestBlockBuilder().build());

  block.addSignature(shared_model::crypto::Signed("data"),
                     shared_model::crypto::PublicKey("key"));

  auto yac_hash = hash_provider.makeHash(block);

  auto model_hash = hash_provider.toModelHash(yac_hash);

  ASSERT_EQ(model_hash, block.hash());
}
