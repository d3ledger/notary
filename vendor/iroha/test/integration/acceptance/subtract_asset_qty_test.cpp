/**
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

#include <gtest/gtest.h>
#include "backend/protobuf/transaction.hpp"
#include "cryptography/crypto_provider/crypto_defaults.hpp"
#include "framework/integration_framework/integration_test_framework.hpp"
#include "integration/acceptance/acceptance_fixture.hpp"
#include "module/shared_model/builders/protobuf/test_transaction_builder.hpp"

using namespace integration_framework;
using namespace shared_model;

class SubtractAssetQuantity : public AcceptanceFixture {
 public:
  /**
   * Creates the transaction with the user creation commands
   * @param perms are the permissions of the user
   * @return built tx and a hash of its payload
   */
  auto makeUserWithPerms(const interface::RolePermissionSet &perms = {
                             interface::permissions::Role::kSubtractAssetQty,
                             interface::permissions::Role::kAddAssetQty}) {
    return AcceptanceFixture::makeUserWithPerms(perms);
  }

  /**
   * @return built tx that adds kAmount assets to the users
   */
  auto replenish() {
    return complete(baseTx().addAssetQuantity(kAsset, kAmount));
  }

  const std::string kAmount = "1.0";
};

/**
 * @given some user with all required permissions
 * @when execute tx with SubtractAssetQuantity command with max available amount
 * @then there is the tx in proposal
 */
TEST_F(SubtractAssetQuantity, Everything) {
  IntegrationTestFramework(1)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms())
      .skipProposal()
      .skipBlock()
      .sendTx(replenish())
      .skipProposal()
      .skipBlock()
      .sendTx(complete(baseTx().subtractAssetQuantity(kAsset, kAmount)))
      .skipProposal()
      .checkBlock(
          [](auto &block) { ASSERT_EQ(block->transactions().size(), 1); })
      .done();
}

/**
 * @given some user with all required permissions
 * @when execute tx with SubtractAssetQuantity command with amount more than
 * user has
 * @then there is no tx in proposal
 */
TEST_F(SubtractAssetQuantity, Overdraft) {
  IntegrationTestFramework(1)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms())
      .skipProposal()
      .skipBlock()
      .sendTx(replenish())
      .skipProposal()
      .skipBlock()
      .sendTx(complete(baseTx().subtractAssetQuantity(kAsset, "2.0")))
      .skipProposal()
      .checkBlock(
          [](auto &block) { ASSERT_EQ(block->transactions().size(), 0); })
      .done();
}

/**
 * @given some user without can_subtract_asset_qty permission
 * @when execute tx with SubtractAssetQuantity command
 * @then there is no tx in proposal
 */
TEST_F(SubtractAssetQuantity, NoPermissions) {
  IntegrationTestFramework(1)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms({interface::permissions::Role::kAddAssetQty}))
      .skipProposal()
      .skipBlock()
      .sendTx(replenish())
      .skipProposal()
      .skipBlock()
      .sendTx(complete(baseTx().subtractAssetQuantity(kAsset, kAmount)))
      .skipProposal()
      .checkBlock(
          [](auto &block) { ASSERT_EQ(block->transactions().size(), 0); })
      .done();
}

/**
 * @given pair of users with all required permissions
 * @when execute tx with SubtractAssetQuantity command with negative amount
 * @then the tx hasn't passed stateless validation
 *       (aka skipProposal throws)
 */
TEST_F(SubtractAssetQuantity, NegativeAmount) {
  IntegrationTestFramework(1)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms())
      .skipProposal()
      .skipBlock()
      .sendTx(replenish())
      .skipProposal()
      .skipBlock()
      .sendTx(complete(baseTx().subtractAssetQuantity(kAsset, "-1.0")),
              checkStatelessInvalid);
}

/**
 * @given pair of users with all required permissions
 * @when execute tx with SubtractAssetQuantity command with zero amount
 * @then the tx hasn't passed stateless validation
 *       (aka skipProposal throws)
 */
TEST_F(SubtractAssetQuantity, ZeroAmount) {
  IntegrationTestFramework(1)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms())
      .skipProposal()
      .skipBlock()
      .sendTx(replenish())
      .skipProposal()
      .skipBlock()
      .sendTx(complete(baseTx().subtractAssetQuantity(kAsset, "0.0")),
              checkStatelessInvalid);
}

/**
 * @given some user with all required permissions
 * @when execute tx with SubtractAssetQuantity command with nonexistent asset
 * @then there is an empty proposal
 */
TEST_F(SubtractAssetQuantity, NonexistentAsset) {
  std::string nonexistent = "inexist#test";
  IntegrationTestFramework(1)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms())
      .skipProposal()
      .skipBlock()
      .sendTx(replenish())
      .skipProposal()
      .skipBlock()
      .sendTx(complete(baseTx().subtractAssetQuantity(nonexistent, kAmount)))
      .skipProposal()
      .checkBlock(
          [](auto &block) { ASSERT_EQ(block->transactions().size(), 0); })
      .done();
}
