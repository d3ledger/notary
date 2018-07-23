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

#include <grpc++/security/server_credentials.h>
#include <grpc++/server.h>
#include <grpc++/server_builder.h>
#include <gtest/gtest.h>

#include "builders/common_objects/peer_builder.hpp"
#include "builders/protobuf/common_objects/proto_peer_builder.hpp"
#include "cryptography/crypto_provider/crypto_defaults.hpp"
#include "cryptography/hash.hpp"
#include "datetime/time.hpp"
#include "framework/test_subscriber.hpp"
#include "module/irohad/ametsuchi/ametsuchi_mocks.hpp"
#include "module/shared_model/builders/protobuf/test_block_builder.hpp"
#include "network/impl/block_loader_impl.hpp"
#include "network/impl/block_loader_service.hpp"
#include "validators/default_validator.hpp"

using namespace iroha::network;
using namespace iroha::ametsuchi;
using namespace framework::test_subscriber;
using namespace shared_model::crypto;

using testing::A;
using testing::Return;

using wPeer = std::shared_ptr<shared_model::interface::Peer>;
using wBlock = std::shared_ptr<shared_model::interface::Block>;

class BlockLoaderTest : public testing::Test {
 public:
  void SetUp() override {
    peer_query = std::make_shared<MockPeerQuery>();
    storage = std::make_shared<MockBlockQuery>();
    loader = std::make_shared<BlockLoaderImpl>(peer_query, storage);
    service = std::make_shared<BlockLoaderService>(storage);

    grpc::ServerBuilder builder;
    int port = 0;
    builder.AddListeningPort(
        "0.0.0.0:0", grpc::InsecureServerCredentials(), &port);
    builder.RegisterService(service.get());
    server = builder.BuildAndStart();

    shared_model::builder::PeerBuilder<
        shared_model::proto::PeerBuilder,
        shared_model::validation::FieldValidator>()
        .address("0.0.0.0:" + std::to_string(port))
        .pubkey(peer_key)
        .build()
        .match(
            [&](iroha::expected::Value<
                std::shared_ptr<shared_model::interface::Peer>> &v) {
              peer = std::move(v.value);
            },
            [](iroha::expected::Error<std::shared_ptr<std::string>>) {});

    ASSERT_TRUE(server);
    ASSERT_NE(port, 0);
  }

  auto getBaseBlockBuilder() const {
    return shared_model::proto::TemplateBlockBuilder<
               (1 << shared_model::proto::TemplateBlockBuilder<>::total) - 1,
               shared_model::validation::AlwaysValidValidator,
               shared_model::proto::UnsignedWrapper<
                   shared_model::proto::Block>>()
        .height(1)
        .prevHash(kPrevHash)
        .createdTime(iroha::time::now());
  }

  const Hash kPrevHash =
      Hash(std::string(DefaultCryptoAlgorithmType::kHashLength, '0'));

  std::shared_ptr<shared_model::interface::Peer> peer;
  PublicKey peer_key =
      DefaultCryptoAlgorithmType::generateKeypair().publicKey();
  Keypair key = DefaultCryptoAlgorithmType::generateKeypair();
  std::shared_ptr<MockPeerQuery> peer_query;
  std::shared_ptr<MockBlockQuery> storage;
  std::shared_ptr<BlockLoaderImpl> loader;
  std::shared_ptr<BlockLoaderService> service;
  std::unique_ptr<grpc::Server> server;
};

/**
 * @given empty storage, related block loader and base block
 * @when retrieveBlocks is called
 * @then nothing is returned
 */
TEST_F(BlockLoaderTest, ValidWhenSameTopBlock) {
  // Current block height 1 => Other block height 1 => no blocks received
  auto block = getBaseBlockBuilder().build().signAndAddSignature(key).finish();

  EXPECT_CALL(*peer_query, getLedgerPeers())
      .WillOnce(Return(std::vector<wPeer>{peer}));
  EXPECT_CALL(*storage, getTopBlock())
      .WillOnce(Return(iroha::expected::makeValue(wBlock(clone(block)))));
  EXPECT_CALL(*storage, getBlocksFrom(block.height() + 1))
      .WillOnce(Return(rxcpp::observable<>::empty<wBlock>()));
  auto wrapper = make_test_subscriber<CallExact>(
      loader->retrieveBlocks(peer->pubkey()), 0);
  wrapper.subscribe();

  ASSERT_TRUE(wrapper.validate());
}

/**
 * @given block loader and a pair of consecutive blocks
 * @when retrieveBlocks is called
 * @then the last one is returned
 */
TEST_F(BlockLoaderTest, ValidWhenOneBlock) {
  // Current block height 1 => Other block height 2 => one block received
  // time validation should work based on the block field
  // so it should pass stateless BlockLoader validation
  auto block = getBaseBlockBuilder()
                   .createdTime(228)
                   .build()
                   .signAndAddSignature(key)
                   .finish();

  auto top_block = getBaseBlockBuilder()
                       .createdTime(block.createdTime() + 1)
                       .height(block.height() + 1)
                       .build()
                       .signAndAddSignature(key)
                       .finish();

  EXPECT_CALL(*peer_query, getLedgerPeers())
      .WillOnce(Return(std::vector<wPeer>{peer}));
  EXPECT_CALL(*storage, getTopBlock())
      .WillOnce(Return(iroha::expected::makeValue(wBlock(clone(block)))));
  EXPECT_CALL(*storage, getBlocksFrom(block.height() + 1))
      .WillOnce(Return(rxcpp::observable<>::just(wBlock(clone(top_block)))));
  auto wrapper =
      make_test_subscriber<CallExact>(loader->retrieveBlocks(peer_key), 1);
  wrapper.subscribe(
      [&top_block](auto block) { ASSERT_EQ(*block.operator->(), top_block); });

  ASSERT_TRUE(wrapper.validate());
}

/**
 * @given block loader, a block, and additional num_blocks blocks
 * @when retrieveBlocks is called
 * @then it returns consecutive heights
 */
TEST_F(BlockLoaderTest, ValidWhenMultipleBlocks) {
  // Current block height 1 => Other block height n => n-1 blocks received
  // time validation should work based on the block field
  // so it should pass stateless BlockLoader validation
  auto block = getBaseBlockBuilder()
                   .createdTime(1337)
                   .build()
                   .signAndAddSignature(key)
                   .finish();

  auto num_blocks = 2;
  auto next_height = block.height() + 1;

  std::vector<wBlock> blocks;
  for (auto i = next_height; i < next_height + num_blocks; ++i) {
    auto blk = getBaseBlockBuilder()
                   .height(i)
                   .build()
                   .signAndAddSignature(key)
                   .finish();
    blocks.emplace_back(clone(blk));
  }

  EXPECT_CALL(*peer_query, getLedgerPeers())
      .WillOnce(Return(std::vector<wPeer>{peer}));
  EXPECT_CALL(*storage, getTopBlock())
      .WillOnce(Return(iroha::expected::makeValue(wBlock(clone(block)))));
  EXPECT_CALL(*storage, getBlocksFrom(next_height))
      .WillOnce(Return(rxcpp::observable<>::iterate(blocks)));
  auto wrapper = make_test_subscriber<CallExact>(
      loader->retrieveBlocks(peer_key), num_blocks);
  auto height = next_height;
  wrapper.subscribe(
      [&height](auto block) { ASSERT_EQ(block->height(), height++); });

  ASSERT_TRUE(wrapper.validate());
}

/**
 * @given block loader with a block
 * @when retrieveBlock is called with the related hash
 * @then it returns the same block
 */
TEST_F(BlockLoaderTest, ValidWhenBlockPresent) {
  // Request existing block => success
  auto requested =
      getBaseBlockBuilder().build().signAndAddSignature(key).finish();

  EXPECT_CALL(*peer_query, getLedgerPeers())
      .WillOnce(Return(std::vector<wPeer>{peer}));
  EXPECT_CALL(*storage, getBlocksFrom(1))
      .WillOnce(Return(rxcpp::observable<>::just(wBlock(clone(requested)))));
  auto block = loader->retrieveBlock(peer_key, requested.hash());

  ASSERT_TRUE(block);
  ASSERT_EQ(**block, requested);
}

/**
 * @given block loader and a block
 * @when retrieveBlock is called with a different hash
 * @then nothing is returned
 */
TEST_F(BlockLoaderTest, ValidWhenBlockMissing) {
  // Request nonexisting block => failure
  auto present =
      getBaseBlockBuilder().build().signAndAddSignature(key).finish();

  EXPECT_CALL(*peer_query, getLedgerPeers())
      .WillOnce(Return(std::vector<wPeer>{peer}));
  EXPECT_CALL(*storage, getBlocksFrom(1))
      .WillOnce(Return(rxcpp::observable<>::just(wBlock(clone(present)))));
  auto block = loader->retrieveBlock(peer_key, kPrevHash);

  ASSERT_FALSE(block);
}
