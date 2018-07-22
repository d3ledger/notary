/**
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

#include <gtest/gtest.h>
#include "backend/protobuf/transaction.hpp"
#include "cryptography/crypto_provider/crypto_defaults.hpp"
#include "datetime/time.hpp"
#include "framework/integration_framework/integration_test_framework.hpp"
#include "integration/acceptance/acceptance_fixture.hpp"
#include "module/shared_model/builders/protobuf/test_transaction_builder.hpp"

using namespace integration_framework;
using namespace shared_model;

class CreateRole : public AcceptanceFixture {
 public:
  auto makeUserWithPerms(const interface::RolePermissionSet &perms = {
                             interface::permissions::Role::kGetMyTxs,
                             interface::permissions::Role::kCreateRole}) {
    return AcceptanceFixture::makeUserWithPerms(kNewRole, perms);
  }

  auto baseTx(const interface::RolePermissionSet &perms,
              const std::string &role_name) {
    return AcceptanceFixture::baseTx().createRole(role_name, perms);
  }

  auto baseTx(const interface::RolePermissionSet &perms = {
                  interface::permissions::Role::kGetMyTxs}) {
    return baseTx(perms, kRole);
  }

  const std::string kNewRole = "rl";
};

/**
 * @given some user with can_create_role permission
 * @when execute tx with CreateRole command
 * @then there is the tx in proposal
 */
TEST_F(CreateRole, Basic) {
  IntegrationTestFramework(1)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms())
      .skipProposal()
      .skipBlock()
      .sendTx(complete(baseTx()))
      .skipProposal()
      .checkBlock(
          [](auto &block) { ASSERT_EQ(block->transactions().size(), 1); })
      .done();
}

/**
 * @given some user without can_create_role permission
 * @when execute tx with CreateRole command
 * @then there is an empty verified proposal
 */
TEST_F(CreateRole, HaveNoPerms) {
  IntegrationTestFramework(1)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms({interface::permissions::Role::kGetMyTxs}))
      .skipProposal()
      .skipBlock()
      .sendTx(complete(baseTx()))
      .checkBlock(
          [](auto &block) { ASSERT_EQ(block->transactions().size(), 0); })
      .done();
}

/**
 * @given some user with can_create_role permission
 * @when execute tx with CreateRole command with empty role
 * @then the tx hasn't passed stateless validation
 *       (aka skipProposal throws)
 */
TEST_F(CreateRole, EmptyRole) {
  IntegrationTestFramework(1)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms())
      .skipProposal()
      .skipBlock()
      .sendTx(complete(baseTx({interface::permissions::Role::kGetMyTxs}, "")),
              checkStatelessInvalid);
}

/**
 * @given some user with can_create_role permission
 * @when execute tx with CreateRole command with empty permission
 * @then the tx hasn't passed stateless validation
 *       (aka skipProposal throws)
 */
TEST_F(CreateRole, EmptyPerms) {
  IntegrationTestFramework(1)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms())
      .skipProposal()
      .skipBlock()
      .sendTxAwait(complete(baseTx({})), [](auto &block) {
        ASSERT_EQ(block->transactions().size(), 1);
      });
}

/**
 * @given some user with can_create_role permission
 * @when execute tx with CreateRole command with too long role name
 * @then the tx hasn't passed stateless validation
 *       (aka skipProposal throws)
 */
TEST_F(CreateRole, LongRoleName) {
  IntegrationTestFramework(1)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms())
      .skipProposal()
      .skipBlock()
      .sendTx(complete(baseTx({interface::permissions::Role::kGetMyTxs},
                              std::string(33, 'a'))),
              checkStatelessInvalid);
}

/**
 * @given some user with can_create_role permission
 * @when execute tx with CreateRole command with maximal role name size
 * @then the tx is comitted
 */
TEST_F(CreateRole, MaxLenRoleName) {
  IntegrationTestFramework(1)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms())
      .skipProposal()
      .skipBlock()
      .sendTx(complete(baseTx({interface::permissions::Role::kGetMyTxs},
                              std::string(32, 'a'))))
      .skipProposal()
      .checkBlock(
          [](auto &block) { ASSERT_EQ(block->transactions().size(), 1); })
      .done();
}

/**
 * TODO 15/05/2018 andrei: IR-1267 fix builders setting default value for
 * nonexisting permissions
 * @given some user with can_create_role permission
 * @when execute tx with CreateRole command with nonexistent permission name
 * @then the tx hasn't passed stateless validation
 *       (aka skipProposal throws)
 */
TEST_F(CreateRole, DISABLED_NonexistentPerm) {
  IntegrationTestFramework(1)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms())
      .skipProposal()
      .skipBlock()
      .sendTx(complete(baseTx({static_cast<interface::permissions::Role>(-1)})),
              checkStatelessInvalid);
}

/**
 * @given some user with can_create_role permission
 * @when execute tx with CreateRole command with existing role name
 * @then there is an empty verified proposal
 */
TEST_F(CreateRole, ExistingRole) {
  IntegrationTestFramework(1)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms())
      .skipProposal()
      .skipBlock()
      .sendTx(
          complete(baseTx({interface::permissions::Role::kGetMyTxs}, kNewRole)))
      .skipProposal()
      .checkBlock(
          [](auto &block) { ASSERT_EQ(block->transactions().size(), 0); });
}
