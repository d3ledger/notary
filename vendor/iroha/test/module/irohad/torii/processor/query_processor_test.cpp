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

#include "backend/protobuf/query_responses/proto_error_query_response.hpp"
#include "builders/protobuf/common_objects/proto_account_builder.hpp"
#include "cryptography/crypto_provider/crypto_defaults.hpp"
#include "cryptography/keypair.hpp"
#include "execution/query_execution.hpp"
#include "framework/test_subscriber.hpp"
#include "framework/specified_visitor.hpp"
#include "module/irohad/ametsuchi/ametsuchi_mocks.hpp"
#include "module/irohad/validation/validation_mocks.hpp"
#include "module/shared_model/builders/protobuf/test_query_builder.hpp"
#include "module/shared_model/builders/protobuf/test_query_response_builder.hpp"
#include "network/ordering_gate.hpp"
#include "torii/processor/query_processor_impl.hpp"
#include "utils/query_error_response_visitor.hpp"
#include "validators/permissions.hpp"

using namespace iroha;
using namespace iroha::ametsuchi;
using namespace iroha::validation;
using namespace framework::test_subscriber;

using ::testing::_;
using ::testing::A;
using ::testing::Return;

class QueryProcessorTest : public ::testing::Test {
 public:
  void SetUp() override {
    created_time = iroha::time::now();
    account_id = "account@domain";
    counter = 1048576;
  }

  decltype(iroha::time::now()) created_time;
  std::string account_id;
  uint64_t counter;
  shared_model::crypto::Keypair keypair =
      shared_model::crypto::DefaultCryptoAlgorithmType::generateKeypair();

  std::vector<shared_model::interface::types::PubkeyType> signatories = {
      keypair.publicKey()};
};

/**
 * @given account, ametsuchi queries and query processing factory
 * @when stateless validation error
 * @then Query Processor should return ErrorQueryResponse
 */
TEST_F(QueryProcessorTest, QueryProcessorWhereInvokeInvalidQuery) {
  auto wsv_queries = std::make_shared<MockWsvQuery>();
  auto block_queries = std::make_shared<MockBlockQuery>();
  auto storage = std::make_shared<MockStorage>();
  auto qpf =
      std::make_unique<QueryProcessingFactory>(wsv_queries, block_queries);

  iroha::torii::QueryProcessorImpl qpi(storage);

  auto query = TestUnsignedQueryBuilder()
                   .createdTime(created_time)
                   .creatorAccountId(account_id)
                   .getAccount(account_id)
                   .queryCounter(counter)
                   .build()
                   .signAndAddSignature(keypair)
                   .finish();

  std::shared_ptr<shared_model::interface::Account> shared_account = clone(
      shared_model::proto::AccountBuilder().accountId(account_id).build());

  auto role = "admin";
  std::vector<std::string> roles = {role};
  std::vector<std::string> perms = {
      shared_model::permissions::can_get_my_account};

  EXPECT_CALL(*storage, getWsvQuery()).WillRepeatedly(Return(wsv_queries));
  EXPECT_CALL(*storage, getBlockQuery()).WillRepeatedly(Return(block_queries));
  EXPECT_CALL(*wsv_queries, getAccount(account_id))
      .WillOnce(Return(shared_account));
  EXPECT_CALL(*wsv_queries, getAccountRoles(account_id))
      .Times(2)
      .WillRepeatedly(Return(roles));
  EXPECT_CALL(*wsv_queries, getRolePermissions(role)).WillOnce(Return(perms));
  EXPECT_CALL(*wsv_queries, getSignatories(account_id))
      .WillRepeatedly(Return(signatories));

  auto wrapper = make_test_subscriber<CallExact>(qpi.queryNotifier(), 1);
  wrapper.subscribe([](auto response) {
    ASSERT_NO_THROW(
        boost::apply_visitor(framework::SpecifiedVisitor<
                                 shared_model::interface::AccountResponse>(),
                             response->get()));
  });
  qpi.queryHandle(
      std::make_shared<shared_model::proto::Query>(query.getTransport()));
  ASSERT_TRUE(wrapper.validate());
}

/**
 * @given account, ametsuchi queries and query processing factory
 * @when signed with wrong key
 * @then Query Processor should return StatefulFailed
 */
TEST_F(QueryProcessorTest, QueryProcessorWithWrongKey) {
  auto wsv_queries = std::make_shared<MockWsvQuery>();
  auto block_queries = std::make_shared<MockBlockQuery>();
  auto storage = std::make_shared<MockStorage>();
  auto qpf =
      std::make_unique<QueryProcessingFactory>(wsv_queries, block_queries);

  iroha::torii::QueryProcessorImpl qpi(storage);

  auto query = TestUnsignedQueryBuilder()
                   .createdTime(created_time)
                   .creatorAccountId(account_id)
                   .getAccount(account_id)
                   .queryCounter(counter)
                   .build()
                   .signAndAddSignature(
                       shared_model::crypto::DefaultCryptoAlgorithmType::
                           generateKeypair())
                   .finish();

  std::shared_ptr<shared_model::interface::Account> shared_account = clone(
      shared_model::proto::AccountBuilder().accountId(account_id).build());
  auto role = "admin";
  std::vector<std::string> roles = {role};
  std::vector<std::string> perms = {
      shared_model::permissions::can_get_my_account};

  EXPECT_CALL(*storage, getWsvQuery()).WillRepeatedly(Return(wsv_queries));
  EXPECT_CALL(*storage, getBlockQuery()).WillRepeatedly(Return(block_queries));
  EXPECT_CALL(*wsv_queries, getSignatories(account_id))
      .WillRepeatedly(Return(signatories));

  auto wrapper = make_test_subscriber<CallExact>(qpi.queryNotifier(), 1);
  wrapper.subscribe([](auto response) {
    ASSERT_TRUE(boost::apply_visitor(
        shared_model::interface::QueryErrorResponseChecker<
            shared_model::interface::StatefulFailedErrorResponse>(),
        response->get()));
  });
  qpi.queryHandle(
      std::make_shared<shared_model::proto::Query>(query.getTransport()));
  ASSERT_TRUE(wrapper.validate());
}
