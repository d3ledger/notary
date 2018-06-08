/**
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

#include <gtest/gtest.h>
#include <boost/property_tree/json_parser.hpp>
#include <boost/property_tree/ptree.hpp>

#include "framework/integration_framework/integration_test_framework.hpp"
#include "framework/specified_visitor.hpp"
#include "integration/acceptance/acceptance_fixture.hpp"
#include "validators/permissions.hpp"

using namespace integration_framework;
using namespace shared_model;

class HeavyTransactionTest : public AcceptanceFixture {
 public:
  /**
   * Creates the transaction with the user creation commands
   * @param perms are the permissions of the user
   * @return built tx and a hash of its payload
   */
  auto makeUserWithPerms(
      const std::vector<std::string> &perms = {
          shared_model::permissions::role_perm_group.begin(),
          shared_model::permissions::role_perm_group.end()}) {
    return AcceptanceFixture::makeUserWithPerms(perms);
  }

  /**
   * Generate stub of transaction for setting data to default user account
   * @param key - key for the insertion
   * @param value - data that will be attached
   * @return generated builder, without signature
   */
  auto setAcountDetailTx(const std::string &key, const std::string &value) {
    return baseTx().setAccountDetail(kUserId, key, value);
  }

  /**
   * Util method for stub data generation
   * @param quantity - number of bytes
   * @return new string with passed quantity length
   */
  static auto generateData(size_t quantity) {
    return std::string(quantity, 'F');
  }

  /**
   * Create valid basis of pre-built query
   * @return query stub with counter, creator and time
   */
  auto baseQuery() {
    return baseQry().queryCounter(1);
  }
};

/**
 * TODO: refactor the test when all stability issues are fixed
 * IR-1264 20/04/2018 neewy
 * @given some user with all required permissions
 * @when send many txes with addAccountDetail with large data inside
 * @then transaction have been passed
 */
TEST_F(HeavyTransactionTest, DISABLED_ManyLargeTxes) {
  auto number_of_txes = 4u;
  IntegrationTestFramework itf(number_of_txes + 1);

  itf.setInitialState(kAdminKeypair).sendTx(makeUserWithPerms());

  for (auto i = 0u; i < number_of_txes; ++i) {
    itf.sendTx(complete(setAcountDetailTx("foo_" + std::to_string(i),
                                          generateData(2 * 1024 * 1024))));
  }
  itf.skipProposal()
      .checkBlock([&](auto &b) {
        ASSERT_EQ(b->transactions().size(), number_of_txes + 1);
      })
      .done();
}

/**
 * TODO: enable the test when performance issues are solved
 * IR-1264 14/05/2018 andrei
 * @given some user with all required permissions
 * @when send tx with many addAccountDetails with large data inside
 * @then transaction is passed
 */
TEST_F(HeavyTransactionTest, DISABLED_VeryLargeTxWithManyCommands) {
  auto big_data = generateData(3 * 1024 * 1024);
  auto large_tx_builder = setAcountDetailTx("foo_1", big_data)
                              .setAccountDetail(kUserId, "foo_2", big_data)
                              .setAccountDetail(kUserId, "foo_3", big_data);

  IntegrationTestFramework(2)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms())
      .sendTx(complete(large_tx_builder))
      .skipProposal()
      .checkBlock(
          [](auto &block) { ASSERT_EQ(block->transactions().size(), 2); })
      .done();
}

/**
 * TODO: disabled until proposal process time in simulator is not optimized
 * and the test freezes (on a proposal verification stage)
 * IR-1264 20/04/2018 neewy
 * @given some user with all required permissions
 * AND max proposal size is 1.
 * @when send txes with addAccountDetail with large data inside.
 * AND transactions are passed stateful validation
 * @then query executed successfully
 */
TEST_F(HeavyTransactionTest, DISABLED_QueryLargeData) {
  auto number_of_times = 15u;
  auto size_of_data = 3 * 1024 * 1024u;
  auto data = generateData(size_of_data);

  auto name_generator = [](auto val) { return "foo_" + std::to_string(val); };

  auto query_checker = [&](auto &status) {
    ASSERT_NO_THROW({
      auto &&response = boost::apply_visitor(
          framework::SpecifiedVisitor<const interface::AccountResponse &>(),
          status.get());

      boost::property_tree::ptree root;
      boost::property_tree::read_json(response.account().jsonData(), root);
      auto user = root.get_child(kUserId);

      ASSERT_EQ(number_of_times, user.size());

      for (auto i = 0u; i < number_of_times; ++i) {
        ASSERT_EQ(data, user.get<std::string>(name_generator(i)));
      }
    });
  };

  IntegrationTestFramework itf(1);
  itf.setInitialState(kAdminKeypair).sendTx(makeUserWithPerms());

  for (auto i = 0u; i < number_of_times; ++i) {
    itf.sendTx(complete(setAcountDetailTx(name_generator(i), data)))
        .skipProposal()
        .checkBlock(
            [](auto &block) { ASSERT_EQ(block->transactions().size(), 1); });
  }

  // The query works fine only with ITF. It doesn't work in production version
  // of Iroha
  itf.sendQuery(complete(baseQuery().getAccount(kUserId)), query_checker)
      .done();
}
