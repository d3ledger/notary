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

#include <boost/filesystem.hpp>
#include <boost/optional.hpp>
#include "ametsuchi/impl/postgres_block_index.hpp"
#include "ametsuchi/impl/postgres_block_query.hpp"
#include "converters/protobuf/json_proto_converter.hpp"
#include "framework/test_subscriber.hpp"
#include "framework/result_fixture.hpp"
#include "module/irohad/ametsuchi/ametsuchi_fixture.hpp"
#include "module/irohad/ametsuchi/ametsuchi_mocks.hpp"
#include "module/shared_model/builders/protobuf/test_block_builder.hpp"
#include "module/shared_model/builders/protobuf/test_transaction_builder.hpp"

using namespace iroha::ametsuchi;
using namespace framework::test_subscriber;

using testing::Return;

class BlockQueryTest : public AmetsuchiTest {
 protected:
  void SetUp() override {
    AmetsuchiTest::SetUp();

    auto tmp = FlatFile::create(block_store_path);
    ASSERT_TRUE(tmp);
    file = std::move(*tmp);
    mock_file = std::make_shared<MockKeyValueStorage>();
    sql = std::make_unique<soci::session>(soci::postgresql, pgopt_);

    index = std::make_shared<PostgresBlockIndex>(*sql);
    blocks = std::make_shared<PostgresBlockQuery>(*sql, *file);
    empty_blocks =
        std::make_shared<PostgresBlockQuery>(*sql, *mock_file);

    *sql << init_;

    // First transaction in block1
    auto txn1_1 = TestTransactionBuilder().creatorAccountId(creator1).build();
    tx_hashes.push_back(txn1_1.hash());

    // Second transaction in block1
    auto txn1_2 = TestTransactionBuilder().creatorAccountId(creator1).build();
    tx_hashes.push_back(txn1_2.hash());

    auto block1 =
        TestBlockBuilder()
            .height(1)
            .transactions(
                std::vector<shared_model::proto::Transaction>({txn1_1, txn1_2}))
            .prevHash(shared_model::crypto::Hash(zero_string))
            .build();

    // First tx in block 1
    auto txn2_1 = TestTransactionBuilder().creatorAccountId(creator1).build();
    tx_hashes.push_back(txn2_1.hash());

    // Second tx in block 2
    auto txn2_2 = TestTransactionBuilder().creatorAccountId(creator2).build();
    tx_hashes.push_back(txn2_2.hash());

    auto block2 =
        TestBlockBuilder()
            .height(2)
            .transactions(
                std::vector<shared_model::proto::Transaction>({txn2_1, txn2_2}))
            .prevHash(block1.hash())
            .build();

    for (const auto &b : {std::move(block1), std::move(block2)}) {
      file->add(b.height(),
                iroha::stringToBytes(
                    shared_model::converters::protobuf::modelToJson(b)));
      index->index(b);
      blocks_total++;
    }
  }

  std::unique_ptr<soci::session> sql;
  std::vector<shared_model::crypto::Hash> tx_hashes;
  std::shared_ptr<BlockQuery> blocks;
  std::shared_ptr<BlockQuery> empty_blocks;
  std::shared_ptr<BlockIndex> index;
  std::unique_ptr<FlatFile> file;
  std::shared_ptr<MockKeyValueStorage> mock_file;
  std::string creator1 = "user1@test";
  std::string creator2 = "user2@test";
  std::size_t blocks_total{0};
  std::string zero_string = std::string(32, '0');
};

/**
 * @given block store with 2 blocks totally containing 3 txs created by
 * user1@test
 * AND 1 tx created by user2@test
 * @when query to get transactions created by user1@test is invoked
 * @then query over user1@test returns 3 txs
 */
TEST_F(BlockQueryTest, GetAccountTransactionsFromSeveralBlocks) {
  // Check that creator1 has created 3 transactions
  auto getCreator1TxWrapper = make_test_subscriber<CallExact>(
      blocks->getAccountTransactions(creator1), 3);
  getCreator1TxWrapper.subscribe(
      [this](auto val) { EXPECT_EQ(val->creatorAccountId(), creator1); });
  ASSERT_TRUE(getCreator1TxWrapper.validate());
}

/**
 * @given block store with 2 blocks totally containing 3 txs created by
 * user1@test
 * AND 1 tx created by user2@test
 * @when query to get transactions created by user2@test is invoked
 * @then query over user2@test returns 1 tx
 */
TEST_F(BlockQueryTest, GetAccountTransactionsFromSingleBlock) {
  // Check that creator1 has created 1 transaction
  auto getCreator2TxWrapper = make_test_subscriber<CallExact>(
      blocks->getAccountTransactions(creator2), 1);
  getCreator2TxWrapper.subscribe(
      [this](auto val) { EXPECT_EQ(val->creatorAccountId(), creator2); });
  ASSERT_TRUE(getCreator2TxWrapper.validate());
}

/**
 * @given block store
 * @when query to get transactions created by user with id not registered in the
 * system is invoked
 * @then query returns empty result
 */
TEST_F(BlockQueryTest, GetAccountTransactionsNonExistingUser) {
  // Check that "nonexisting" user has no transaction
  auto getNonexistingTxWrapper = make_test_subscriber<CallExact>(
      blocks->getAccountTransactions("nonexisting user"), 0);
  getNonexistingTxWrapper.subscribe();
  ASSERT_TRUE(getNonexistingTxWrapper.validate());
}

/**
 * @given block store with 2 blocks totally containing 3 txs created by
 * user1@test
 * AND 1 tx created by user2@test
 * @when query to get transactions with existing transaction hashes
 * @then queried transactions
 */
TEST_F(BlockQueryTest, GetTransactionsExistingTxHashes) {
  auto wrapper = make_test_subscriber<CallExact>(
      blocks->getTransactions({tx_hashes[1], tx_hashes[3]}), 2);
  wrapper.subscribe([this](auto tx) {
    static auto subs_cnt = 0;
    subs_cnt++;
    if (subs_cnt == 1) {
      ASSERT_TRUE(tx);
      EXPECT_EQ(tx_hashes[1], (*tx)->hash());
    } else {
      ASSERT_TRUE(tx);
      EXPECT_EQ(tx_hashes[3], (*tx)->hash());
    }
  });
  ASSERT_TRUE(wrapper.validate());
}

/**
 * @given block store with 2 blocks totally containing 3 txs created by
 * user1@test
 * AND 1 tx created by user2@test
 * @when query to get transactions with non-existing transaction hashes
 * @then nullopt values are retrieved
 */
TEST_F(BlockQueryTest, GetTransactionsIncludesNonExistingTxHashes) {
  shared_model::crypto::Hash invalid_tx_hash_1(zero_string),
      invalid_tx_hash_2(std::string(
          shared_model::crypto::DefaultCryptoAlgorithmType::kHashLength, '9'));
  auto wrapper = make_test_subscriber<CallExact>(
      blocks->getTransactions({invalid_tx_hash_1, invalid_tx_hash_2}), 2);
  wrapper.subscribe(
      [](auto transaction) { EXPECT_EQ(boost::none, transaction); });
  ASSERT_TRUE(wrapper.validate());
}

/**
 * @given block store with 2 blocks totally containing 3 txs created by
 * user1@test
 * AND 1 tx created by user2@test
 * @when query to get transactions with empty vector
 * @then no transactions are retrieved
 */
TEST_F(BlockQueryTest, GetTransactionsWithEmpty) {
  // transactions' hashes are empty.
  auto wrapper =
      make_test_subscriber<CallExact>(blocks->getTransactions({}), 0);
  wrapper.subscribe();
  ASSERT_TRUE(wrapper.validate());
}

/**
 * @given block store with 2 blocks totally containing 3 txs created by
 * user1@test
 * AND 1 tx created by user2@test
 * @when query to get transactions with non-existing txhash and existing txhash
 * @then queried transactions and empty transaction
 */
TEST_F(BlockQueryTest, GetTransactionsWithInvalidTxAndValidTx) {
  // TODO 15/11/17 motxx - Use EqualList VerificationStrategy
  shared_model::crypto::Hash invalid_tx_hash_1(zero_string);
  auto wrapper = make_test_subscriber<CallExact>(
      blocks->getTransactions({invalid_tx_hash_1, tx_hashes[0]}), 2);
  wrapper.subscribe([this](auto tx) {
    static auto subs_cnt = 0;
    subs_cnt++;
    if (subs_cnt == 1) {
      EXPECT_EQ(boost::none, tx);
    } else {
      EXPECT_TRUE(tx);
      EXPECT_EQ(tx_hashes[0], (*tx)->hash());
    }
  });
  ASSERT_TRUE(wrapper.validate());
}

/**
 * @given block store with 2 blocks totally containing 3 txs created by
 * user1@test AND 1 tx created by user2@test
 * @when get non-existent 1000th block
 * @then nothing is returned
 */
TEST_F(BlockQueryTest, GetNonExistentBlock) {
  auto wrapper = make_test_subscriber<CallExact>(blocks->getBlocks(1000, 1), 0);
  wrapper.subscribe();
  ASSERT_TRUE(wrapper.validate());
}

/**
 * @given block store with 2 blocks totally containing 3 txs created by
 * user1@test AND 1 tx created by user2@test
 * @when height=1, count=1
 * @then returned exactly 1 block
 */
TEST_F(BlockQueryTest, GetExactlyOneBlock) {
  auto wrapper = make_test_subscriber<CallExact>(blocks->getBlocks(1, 1), 1);
  wrapper.subscribe();
  ASSERT_TRUE(wrapper.validate());
}

/**
 * @given block store with 2 blocks totally containing 3 txs created by
 * user1@test AND 1 tx created by user2@test
 * @when count=0
 * @then no blocks returned
 */
TEST_F(BlockQueryTest, GetBlocks_Count0) {
  auto wrapper = make_test_subscriber<CallExact>(blocks->getBlocks(1, 0), 0);
  wrapper.subscribe();
  ASSERT_TRUE(wrapper.validate());
}

/**
 * @given block store with 2 blocks totally containing 3 txs created by
 * user1@test AND 1 tx created by user2@test
 * @when get zero block
 * @then no blocks returned
 */
TEST_F(BlockQueryTest, GetZeroBlock) {
  auto wrapper = make_test_subscriber<CallExact>(blocks->getBlocks(0, 1), 0);
  wrapper.subscribe();
  ASSERT_TRUE(wrapper.validate());
}

/**
 * @given block store with 2 blocks totally containing 3 txs created by
 * user1@test AND 1 tx created by user2@test
 * @when get all blocks starting from 1
 * @then returned all blocks (2)
 */
TEST_F(BlockQueryTest, GetBlocksFrom1) {
  auto wrapper =
      make_test_subscriber<CallExact>(blocks->getBlocksFrom(1), blocks_total);
  size_t counter = 1;
  wrapper.subscribe([&counter](const auto &b) {
    // wrapper returns blocks 1 and 2
    ASSERT_EQ(b->height(), counter++)
        << "block height: " << b->height() << "counter: " << counter;
  });
  ASSERT_TRUE(wrapper.validate());
}

/**
 * @given block store with 2 blocks totally containing 3 txs created by
 * user1@test AND 1 tx created by user2@test. Block #1 is filled with trash data
 * (NOT JSON).
 * @when read block #1
 * @then get no blocks
 */
TEST_F(BlockQueryTest, GetBlockButItIsNotJSON) {
  namespace fs = boost::filesystem;
  size_t block_n = 1;

  // write something that is NOT JSON to block #1
  auto block_path = fs::path{block_store_path} / FlatFile::id_to_name(block_n);
  fs::ofstream block_file(block_path);
  std::string content = R"(this is definitely not json)";
  block_file << content;
  block_file.close();

  auto wrapper =
      make_test_subscriber<CallExact>(blocks->getBlocks(block_n, 1), 0);
  wrapper.subscribe();

  ASSERT_TRUE(wrapper.validate());
}

/**
 * @given block store with 2 blocks totally containing 3 txs created by
 * user1@test AND 1 tx created by user2@test. Block #1 is filled with trash data
 * (NOT JSON).
 * @when read block #1
 * @then get no blocks
 */
TEST_F(BlockQueryTest, GetBlockButItIsInvalidBlock) {
  namespace fs = boost::filesystem;
  size_t block_n = 1;

  // write bad block instead of block #1
  auto block_path = fs::path{block_store_path} / FlatFile::id_to_name(block_n);
  fs::ofstream block_file(block_path);
  std::string content = R"({
  "testcase": [],
  "description": "make sure this is valid json, but definitely not a block"
})";
  block_file << content;
  block_file.close();

  auto wrapper =
      make_test_subscriber<CallExact>(blocks->getBlocks(block_n, 1), 0);
  wrapper.subscribe();

  ASSERT_TRUE(wrapper.validate());
}

/**
 * @given block store with 2 blocks totally containing 3 txs created by
 * user1@test AND 1 tx created by user2@test
 * @when get top 2 blocks
 * @then last 2 blocks returned with correct height
 */
TEST_F(BlockQueryTest, GetTop2Blocks) {
  size_t blocks_n = 2;  // top 2 blocks
  auto wrapper =
      make_test_subscriber<CallExact>(blocks->getTopBlocks(blocks_n), blocks_n);

  size_t counter = blocks_total - blocks_n + 1;
  wrapper.subscribe(
      [&counter](const auto &b) { ASSERT_EQ(b->height(), counter++); });

  ASSERT_TRUE(wrapper.validate());
}

/**
 * @given block store with preinserted blocks
 * @when hasTxWithHash is invoked on existing transaction hash
 * @then True is returned
 */
TEST_F(BlockQueryTest, HasTxWithExistingHash) {
  for (const auto &hash : tx_hashes) {
    EXPECT_TRUE(blocks->hasTxWithHash(hash));
  }
}

/**
 * @given block store with preinserted blocks
 * user1@test AND 1 tx created by user2@test
 * @when hasTxWithHash is invoked on non-existing hash
 * @then False is returned
 */
TEST_F(BlockQueryTest, HasTxWithInvalidHash) {
  shared_model::crypto::Hash invalid_tx_hash(zero_string);
  EXPECT_FALSE(blocks->hasTxWithHash(invalid_tx_hash));
}

/**
 * @given block store with preinserted blocks
 * @when getTopBlock is invoked on this block store
 * @then returned top block's height is equal to the inserted one's
 */
TEST_F(BlockQueryTest, GetTopBlockSuccess) {
  auto top_block_opt = framework::expected::val(blocks->getTopBlock());
  ASSERT_TRUE(top_block_opt);
  ASSERT_EQ(top_block_opt.value().value->height(), 2);
}

/**
 * @given empty block store
 * @when getTopBlock is invoked on this block store
 * @then result must be a string error, because no block was fetched
 */
TEST_F(BlockQueryTest, GetTopBlockFail) {
  EXPECT_CALL(*mock_file, last_id()).WillRepeatedly(Return(0));
  EXPECT_CALL(*mock_file, get(mock_file->last_id()))
      .WillOnce(Return(boost::none));

  auto top_block_error = framework::expected::err(empty_blocks->getTopBlock());
  ASSERT_TRUE(top_block_error);
  ASSERT_EQ(top_block_error.value().error, "error while fetching the last block");
}
