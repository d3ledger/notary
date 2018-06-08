/**
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

#include "framework/integration_framework/integration_test_framework.hpp"
#include "framework/specified_visitor.hpp"
#include "integration/acceptance/acceptance_fixture.hpp"
#include "validators/permissions.hpp"

using namespace integration_framework;
using namespace shared_model;

class QueryAcceptanceTest : public AcceptanceFixture {
 public:
  /**
   * Creates the transaction with the user creation commands
   * @param perms are the permissions of the user
   * @return built tx and a hash of its payload
   */
  auto makeUserWithPerms(const std::vector<std::string> &perms = {
                             shared_model::permissions::can_get_my_txs}) {
    auto new_perms = perms;
    new_perms.push_back(shared_model::permissions::can_set_quorum);
    return AcceptanceFixture::makeUserWithPerms(kNewRole, new_perms);
  }

  /**
   * Valid transaction that user can execute.
   * @return built tx and a hash of its payload
   * Note: It should affect the ledger minimally
   */
  auto dummyTx() {
    return complete(AcceptanceFixture::baseTx().setAccountQuorum(kUserId, 1));
  }

  /**
   * Creates valid GetTransactions query of current user
   * @param hash of the tx for querying
   * @return built query
   */
  auto makeQuery(const crypto::Hash &hash) {
    return complete(baseQry().queryCounter(1).getTransactions(
        std::vector<crypto::Hash>{hash}));
  }

  const std::string kNewRole = "rl";
};

/**
 * @given some user with only can_get_my_txs permission
 * @when query GetTransactions of existing transaction of the user in parallel
 * @then receive TransactionsResponse with the transaction hash
 */
TEST_F(QueryAcceptanceTest, ParallelBlockQuery) {
  auto dummy_tx = dummyTx();
  auto check = [&dummy_tx](auto &status) {
    ASSERT_NO_THROW({
      const auto &resp = boost::apply_visitor(
          framework::SpecifiedVisitor<interface::TransactionsResponse>(),
          status.get());
      ASSERT_EQ(resp.transactions().size(), 1);
      ASSERT_EQ(resp.transactions().front(), dummy_tx);
    });
  };

  IntegrationTestFramework itf(2);
  itf.setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms())
      .sendTx(dummy_tx)
      .checkBlock(
          [](auto &block) { ASSERT_EQ(block->transactions().size(), 2); });

  const auto num_queries = 5;

  auto send_query = [&] {
    for (int i = 0; i < num_queries; ++i) {
      itf.sendQuery(makeQuery(dummy_tx.hash()), check);
    }
  };

  const auto num_threads = 5;

  std::vector<std::thread> threads;
  for (int i = 0; i < num_threads; ++i) {
    threads.emplace_back(send_query);
  }

  for (auto &thread : threads) {
    thread.join();
  }

  itf.done();
}
