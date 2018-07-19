/**
 * Copyright Soramitsu Co., Ltd. 2018 All Rights Reserved.
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

#include <memory>
#include <rxcpp/rx-observable.hpp>

#include "builders/protobuf/block.hpp"
#include "builders/protobuf/common_objects/proto_signature_builder.hpp"
#include "builders/protobuf/transaction.hpp"
#include "consensus/yac/impl/yac_gate_impl.hpp"
#include "consensus/yac/storage/yac_proposal_storage.hpp"
#include "cryptography/crypto_provider/crypto_defaults.hpp"
#include "framework/specified_visitor.hpp"
#include "framework/test_subscriber.hpp"
#include "module/irohad/consensus/yac/yac_mocks.hpp"
#include "module/irohad/network/network_mocks.hpp"
#include "module/irohad/simulator/simulator_mocks.hpp"

using namespace iroha::consensus::yac;
using namespace iroha::network;
using namespace iroha::simulator;
using namespace framework::test_subscriber;
using namespace shared_model::crypto;
using namespace std;

using ::testing::_;
using ::testing::An;
using ::testing::AtLeast;
using ::testing::Return;

class YacGateTest : public ::testing::Test {
 public:
  void SetUp() override {
    auto keypair =
        shared_model::crypto::DefaultCryptoAlgorithmType::generateKeypair();

    expected_hash = YacHash("proposal", "block");
    auto tx = shared_model::proto::TransactionBuilder()
                  .creatorAccountId("account@domain")
                  .setAccountQuorum("account@domain", 1)
                  .createdTime(iroha::time::now())
                  .quorum(1)
                  .build()
                  .signAndAddSignature(keypair);
    shared_model::proto::Block tmp =
        shared_model::proto::BlockBuilder()
            .height(1)
            .createdTime(iroha::time::now())
            .transactions(std::vector<shared_model::proto::Transaction>{})
            .prevHash(Sha3_256::makeHash(Blob("block")))
            .build()
            .signAndAddSignature(keypair)
            .finish();

    expected_block = clone(tmp);
    const auto &signature = *(expected_block->signatures().begin());

    expected_hash.block_signature = clone(signature);
    message.hash = expected_hash;
    message.signature = clone(signature);
    commit_message = CommitMessage({message});
    expected_commit = rxcpp::observable<>::just(commit_message);

    hash_gate = make_unique<MockHashGate>();
    peer_orderer = make_unique<MockYacPeerOrderer>();
    hash_provider = make_shared<MockYacHashProvider>();
    block_creator = make_shared<MockBlockCreator>();
    block_loader = make_shared<MockBlockLoader>();
  }

  void init() {
    gate = std::make_shared<YacGateImpl>(std::move(hash_gate),
                                         std::move(peer_orderer),
                                         hash_provider,
                                         block_creator,
                                         block_loader,
                                         delay);
  }

  YacHash expected_hash;
  std::shared_ptr<shared_model::interface::Block> expected_block;
  VoteMessage message;
  CommitMessage commit_message;
  rxcpp::observable<CommitMessage> expected_commit;

  unique_ptr<MockHashGate> hash_gate;
  unique_ptr<MockYacPeerOrderer> peer_orderer;
  shared_ptr<MockYacHashProvider> hash_provider;
  shared_ptr<MockBlockCreator> block_creator;
  shared_ptr<MockBlockLoader> block_loader;
  uint64_t delay = 0;

  shared_ptr<YacGateImpl> gate;

 protected:
  YacGateTest() : commit_message(std::vector<VoteMessage>{}) {}
};

TEST_F(YacGateTest, YacGateSubscriptionTest) {
  cout << "----------| BlockCreator (block)=> YacGate (vote)=> "
          "HashGate (commit) => YacGate => on_commit() |----------"
       << endl;

  // yac consensus
  EXPECT_CALL(*hash_gate, vote(expected_hash, _)).Times(1);

  EXPECT_CALL(*hash_gate, on_commit()).WillOnce(Return(expected_commit));

  // generate order of peers
  EXPECT_CALL(*peer_orderer, getOrdering(_))
      .WillOnce(Return(ClusterOrdering::create({mk_peer("fake_node")})));

  // make hash from block
  EXPECT_CALL(*hash_provider, makeHash(_)).WillOnce(Return(expected_hash));

  // make blocks
  EXPECT_CALL(*block_creator, on_block())
      .WillOnce(Return(
          rxcpp::observable<>::just<shared_model::interface::BlockVariant>(
              expected_block)));

  init();

  // verify that yac gate emit expected block
  auto gate_wrapper = make_test_subscriber<CallExact>(gate->on_commit(), 1);
  gate_wrapper.subscribe([this](const auto &block_variant) {
    ASSERT_NO_THROW({
      auto block = boost::apply_visitor(
          framework::SpecifiedVisitor<decltype(expected_block)>(),
          block_variant);
      ASSERT_EQ(*block, *expected_block);
    });
  });

  ASSERT_TRUE(gate_wrapper.validate());
}

TEST_F(YacGateTest, YacGateSubscribtionTestFailCase) {
  cout << "----------| Fail case of retrieving cluster order  |----------"
       << endl;

  // yac consensus
  EXPECT_CALL(*hash_gate, vote(_, _)).Times(0);

  EXPECT_CALL(*hash_gate, on_commit()).Times(0);

  // generate order of peers
  EXPECT_CALL(*peer_orderer, getOrdering(_)).WillOnce(Return(boost::none));

  // make hash from block
  EXPECT_CALL(*hash_provider, makeHash(_)).WillOnce(Return(expected_hash));

  // make blocks
  EXPECT_CALL(*block_creator, on_block())
      .WillOnce(Return(
          rxcpp::observable<>::just<shared_model::interface::BlockVariant>(
              expected_block)));

  init();
}

TEST_F(YacGateTest, LoadBlockWhenDifferentCommit) {
  // Vote for block => receive different block => load committed block

  // make blocks
  EXPECT_CALL(*block_creator, on_block())
      .WillOnce(Return(
          rxcpp::observable<>::just<shared_model::interface::BlockVariant>(
              expected_block)));

  // make hash from block
  EXPECT_CALL(*hash_provider, makeHash(_)).WillOnce(Return(expected_hash));

  // generate order of peers
  EXPECT_CALL(*peer_orderer, getOrdering(_))
      .WillOnce(Return(ClusterOrdering::create({mk_peer("fake_node")})));

  EXPECT_CALL(*hash_gate, vote(expected_hash, _)).Times(1);

  // expected values
  expected_hash = YacHash("actual_proposal", "actual_block");

  message.hash = expected_hash;

  commit_message = CommitMessage({message});
  expected_commit = rxcpp::observable<>::just(commit_message);

  // yac consensus
  EXPECT_CALL(*hash_gate, on_commit()).WillOnce(Return(expected_commit));

  // convert yac hash to model hash
  EXPECT_CALL(*hash_provider, toModelHash(expected_hash))
      .WillOnce(Return(expected_block->hash()));

  // load block
  auto sig = expected_block->signatures().begin();
  auto &pubkey = sig->publicKey();
  EXPECT_CALL(*block_loader, retrieveBlock(pubkey, expected_block->hash()))
      .WillOnce(Return(expected_block));

  init();

  // verify that yac gate emit expected block
  auto gate_wrapper = make_test_subscriber<CallExact>(gate->on_commit(), 1);
  gate_wrapper.subscribe([this](const auto &block_variant) {
    ASSERT_NO_THROW({
      auto block = boost::apply_visitor(
          framework::SpecifiedVisitor<decltype(expected_block)>(),
          block_variant);
      ASSERT_EQ(*block, *expected_block);
    });
  });

  ASSERT_TRUE(gate_wrapper.validate());
}

/**
 * @given yac gate
 * @when recives new commit different to the one it voted for
 * @then polls nodes for the block with corresponding hash until it succeed,
 * (reciving none on the first poll)
 */
TEST_F(YacGateTest, LoadBlockWhenDifferentCommitFailFirst) {
  // Vote for block => receive different block => load committed block

  // make blocks
  EXPECT_CALL(*block_creator, on_block())
      .WillOnce(Return(
          rxcpp::observable<>::just<shared_model::interface::BlockVariant>(
              expected_block)));

  // make hash from block
  EXPECT_CALL(*hash_provider, makeHash(_)).WillOnce(Return(expected_hash));

  // generate order of peers
  EXPECT_CALL(*peer_orderer, getOrdering(_))
      .WillOnce(Return(ClusterOrdering::create({mk_peer("fake_node")})));

  EXPECT_CALL(*hash_gate, vote(expected_hash, _)).Times(1);

  // expected values
  expected_hash = YacHash("actual_proposal", "actual_block");

  message.hash = expected_hash;

  commit_message = CommitMessage({message});
  expected_commit = rxcpp::observable<>::just(commit_message);

  // yac consensus
  EXPECT_CALL(*hash_gate, on_commit()).WillOnce(Return(expected_commit));

  // convert yac hash to model hash
  EXPECT_CALL(*hash_provider, toModelHash(expected_hash))
      .WillOnce(Return(expected_block->hash()));

  // load block
  auto sig = expected_block->signatures().begin();
  auto &pubkey = sig->publicKey();
  EXPECT_CALL(*block_loader, retrieveBlock(pubkey, expected_block->hash()))
      .WillOnce(Return(boost::none))
      .WillOnce(Return(expected_block));

  init();

  // verify that yac gate emit expected block
  auto gate_wrapper = make_test_subscriber<CallExact>(gate->on_commit(), 1);
  gate_wrapper.subscribe([this](const auto &block_variant) {
    ASSERT_NO_THROW({
      auto block = boost::apply_visitor(
          framework::SpecifiedVisitor<decltype(expected_block)>(),
          block_variant);
      ASSERT_EQ(*block, *expected_block);
    });
  });

  ASSERT_TRUE(gate_wrapper.validate());
}
