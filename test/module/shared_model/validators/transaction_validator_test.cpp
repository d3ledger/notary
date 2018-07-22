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

#include "module/shared_model/validators/validators_fixture.hpp"

#include <gtest/gtest.h>
#include <boost/range/irange.hpp>

#include <type_traits>
#include "builders/protobuf/transaction.hpp"
#include "module/shared_model/builders/protobuf/test_transaction_builder.hpp"

using namespace shared_model;

class TransactionValidatorTest : public ValidatorsTest {
 protected:
  iroha::protocol::Transaction generateEmptyTransaction() {
    std::string creator_account_id = "admin@test";

    TestTransactionBuilder builder;
    auto tx = builder.creatorAccountId(creator_account_id)
        .createdTime(created_time)
        .quorum(1)
        .build()
        .getTransport();
    return tx;
  }
  shared_model::validation::DefaultTransactionValidator transaction_validator;
};

/**
 * @given transaction without any commands
 * @when commands validator is invoked
 * @then answer has error about empty transaction
 */
TEST_F(TransactionValidatorTest, EmptyTransactionTest) {
  auto tx = generateEmptyTransaction();
  tx.mutable_payload()->mutable_reduced_payload()->set_created_time(
      created_time);
  auto result = proto::Transaction(iroha::protocol::Transaction(tx));
  auto answer = transaction_validator.validate(result);
  ASSERT_EQ(answer.getReasonsMap().size(), 1);
}

/**
 * @given transaction without any commands
 * @when commands validator is invoked
 * @then answer has error about empty transaction
 */
TEST_F(TransactionValidatorTest, InvalidCreateRolePermission) {
  auto tx = generateEmptyTransaction();
  tx.mutable_payload()->mutable_reduced_payload()->set_created_time(
      created_time);
  iroha::protocol::Command cmd;
  cmd.mutable_create_role()->set_role_name("role");
  cmd.mutable_create_role()->add_permissions(
      static_cast<iroha::protocol::RolePermission>(-1));
  *tx.mutable_payload()->mutable_reduced_payload()->add_commands() =
      std::move(cmd);
  shared_model::validation::DefaultTransactionValidator transaction_validator;
  auto result = proto::Transaction(iroha::protocol::Transaction(tx));
  auto answer = transaction_validator.validate(result);
  ASSERT_EQ(answer.getReasonsMap().size(), 1);
}

/**
 * @given transaction made of commands with valid fields
 * @when commands validation is invoked
 * @then answer has no errors
 */
TEST_F(TransactionValidatorTest, StatelessValidTest) {
  iroha::protocol::Transaction tx = generateEmptyTransaction();
  tx.mutable_payload()->mutable_reduced_payload()->set_creator_account_id(
      account_id);
  tx.mutable_payload()->mutable_reduced_payload()->set_created_time(
      created_time);
  auto payload = tx.mutable_payload();

  // Iterate through all command types, filling command fields with valid values
  iterateContainer(
      [] { return iroha::protocol::Command::descriptor(); },
      [&](auto field) {
        // Add new command to transaction
        auto command = payload->mutable_reduced_payload()->add_commands();
        // Set concrete type for new command
        return command->GetReflection()->MutableMessage(command, field);
      },
      [this](auto field, auto command) {
        // Will throw key exception in case new field is added
        field_setters.at(field->name())(
            command->GetReflection(), command, field);
      },
      [] {});

  auto result = proto::Transaction(iroha::protocol::Transaction(tx));
  auto answer = transaction_validator.validate(result);

  ASSERT_FALSE(answer.hasErrors()) << answer.reason();
}

/**
 * @given Protobuf transaction object with unset command
 * @when validate is called
 * @then there is a error returned
 */
TEST_F(TransactionValidatorTest, UnsetCommand) {
  iroha::protocol::Transaction tx = generateEmptyTransaction();
  tx.mutable_payload()->mutable_reduced_payload()->set_creator_account_id(
      account_id);
  tx.mutable_payload()->mutable_reduced_payload()->set_created_time(
      created_time);
  auto answer = transaction_validator.validate(proto::Transaction(tx));
  tx.mutable_payload()->mutable_reduced_payload()->add_commands();
  ASSERT_TRUE(answer.hasErrors());
}

/**
 * @given transaction made of commands with invalid fields
 * @when commands validation is invoked
 * @then answer has errors and number of errors in answer is the same as the
 * number of commands in tx
 */
TEST_F(TransactionValidatorTest, StatelessInvalidTest) {
  iroha::protocol::Transaction tx = generateEmptyTransaction();
  auto payload = tx.mutable_payload();

  iroha::ts64_t invalid_time = 10000000000ull;
  payload->mutable_reduced_payload()->set_created_time(invalid_time);

  // create commands from default constructors, which will have empty, therefore
  // invalid values
  iterateContainer(
      [] { return iroha::protocol::Command::descriptor(); },
      [&](auto field) {
        // Add new command to transaction
        auto command = payload->mutable_reduced_payload()->add_commands();
        // Set concrete type for new command
        return command->GetReflection()->MutableMessage(command, field);
      },
      [](auto, auto) {
        // Note that no fields are set
      },
      [] {});

  auto result = proto::Transaction(iroha::protocol::Transaction(tx));
  auto answer = transaction_validator.validate(result);

  // in total there should be number_of_commands + 1 reasons of bad answer:
  // number_of_commands for each command + 1 for transaction metadata
  ASSERT_EQ(answer.getReasonsMap().size(),
            iroha::protocol::Command::descriptor()->field_count() + 1);
}
/**
 * @given transaction made of commands with valid fields
 * @when commands validation is invoked
 * @then answer has no errors
 */
TEST_F(TransactionValidatorTest, BatchValidTest) {
  std::string creator_account_id = "admin@test";

  TestTransactionBuilder builder;
  auto tx = builder.creatorAccountId(creator_account_id)
      .createdTime(created_time)
      .quorum(1)
      .batchMeta(interface::types::BatchType::ATOMIC,
                 std::vector<interface::types::HashType>())
      .createDomain("test", "test")
      .build()
      .getTransport();
  shared_model::validation::DefaultTransactionValidator transaction_validator;
  auto result = proto::Transaction(iroha::protocol::Transaction(tx));
  auto answer = transaction_validator.validate(result);

  ASSERT_FALSE(answer.hasErrors()) << answer.reason();
  ASSERT_EQ(tx.payload().batch().type(),
            static_cast<int>(interface::types::BatchType::ATOMIC));
}

