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

#include <gtest/gtest.h>
#include "builders/default_builders.hpp"
#include "builders/transaction_responses/transaction_status_builder.hpp"
#include "framework/specified_visitor.hpp"

using shared_model::builder::TransactionStatusBuilder;

using BuilderType =
    TransactionStatusBuilder<shared_model::proto::TransactionStatusBuilder>;

template <typename T>
class TransactionResponseBuilderTest : public ::testing::Test {};

template <typename Iface, BuilderType (BuilderType::*Member)()>
struct TransactionResponseBuilderTestCase {
  using IfaceType = Iface;
  static constexpr auto member = Member;
};

using TransactionResponsTypes =
    ::testing::Types<TransactionResponseBuilderTestCase<
                         shared_model::interface::StatelessFailedTxResponse,
                         &BuilderType::statelessValidationFailed>,
                     TransactionResponseBuilderTestCase<
                         shared_model::interface::StatelessValidTxResponse,
                         &BuilderType::statelessValidationSuccess>,
                     TransactionResponseBuilderTestCase<
                         shared_model::interface::StatefulFailedTxResponse,
                         &BuilderType::statefulValidationFailed>,
                     TransactionResponseBuilderTestCase<
                         shared_model::interface::StatefulValidTxResponse,
                         &BuilderType::statefulValidationSuccess>,
                     TransactionResponseBuilderTestCase<
                         shared_model::interface::CommittedTxResponse,
                         &BuilderType::committed>,
                     TransactionResponseBuilderTestCase<
                         shared_model::interface::MstExpiredResponse,
                         &BuilderType::mstExpired>,
                     TransactionResponseBuilderTestCase<
                         shared_model::interface::NotReceivedTxResponse,
                         &BuilderType::notReceived> >;
TYPED_TEST_CASE(TransactionResponseBuilderTest, TransactionResponsTypes);

/**
 * @given expected transaction status and hash
 * @when model object is built using these status and hash
 * @then built object has expected status and hash
 */
TYPED_TEST(TransactionResponseBuilderTest, StatusType) {
  using StatusType = typename TypeParam::IfaceType;

  auto expected_hash = shared_model::crypto::Hash(std::string(32, '1'));

  auto response =
      (BuilderType().*TypeParam::member)().txHash(expected_hash).build();

  // check if type in response is as expected
  ASSERT_NO_THROW(boost::apply_visitor(
      framework::SpecifiedVisitor<StatusType>(),
      response->get()));

  ASSERT_EQ(response->transactionHash(), expected_hash);
}

/**
 * @given fields for transaction status object
 * @when TransactionStatusBuilder is invoked twice with the same configuration
 * @then Two constructed TransactionStatus objects are identical
 */
TEST(ProtoTransactionStatusBuilderTest, SeveralObjectsFromOneBuilder) {
  using NotReceivedStatusType = shared_model::interface::NotReceivedTxResponse;

  auto expected_hash = shared_model::crypto::Hash(std::string(32, '1'));

  auto state = BuilderType().notReceived().txHash(expected_hash);

  auto response1 = state.build();
  auto response2 = state.build();

  ASSERT_EQ(*response1, *response2);
  ASSERT_EQ(response1->transactionHash(), expected_hash);

  ASSERT_NO_THROW(boost::apply_visitor(
      framework::SpecifiedVisitor<NotReceivedStatusType>(),
      response1->get()));
}
