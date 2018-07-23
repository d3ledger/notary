/**
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

#include <gtest/gtest.h>

#include "backend/protobuf/common_objects/proto_common_objects_factory.hpp"
#include "builders/default_builders.hpp"
#include "cryptography/crypto_provider/crypto_defaults.hpp"
#include "framework/result_fixture.hpp"
#include "validators/field_validator.hpp"

using namespace shared_model;
using namespace framework::expected;

proto::ProtoCommonObjectsFactory<validation::FieldValidator> factory;

class PeerTest : public ::testing::Test {
 public:
  std::string valid_address = "127.0.0.1:8080";
  crypto::PublicKey valid_pubkey =
      crypto::DefaultCryptoAlgorithmType::generateKeypair().publicKey();
  std::string invalid_address = "127.0.0.1";
};

/**
 * @given valid data for peer
 * @when peer is created via factory
 * @then peer is successfully initialized
 */
TEST_F(PeerTest, ValidPeerInitialization) {
  auto peer = factory.createPeer(valid_address, valid_pubkey);

  peer.match(
      [&](const ValueOf<decltype(peer)> &v) {
        ASSERT_EQ(v.value->address(), valid_address);
        ASSERT_EQ(v.value->pubkey().hex(), valid_pubkey.hex());
      },
      [](const ErrorOf<decltype(peer)> &e) { FAIL() << e.error; });
}

/**
 * @given invalid data for peer
 * @when peer is created via factory
 * @then peer is not initialized correctly
 */
TEST_F(PeerTest, InvalidPeerInitialization) {
  auto peer = factory.createPeer(invalid_address, valid_pubkey);

  peer.match(
      [](const ValueOf<decltype(peer)> &v) { FAIL() << "Expected error case"; },
      [](const ErrorOf<decltype(peer)> &e) { SUCCEED(); });
}

class AccountTest : public ::testing::Test {
 public:
  interface::types::AccountIdType valid_account_id = "hello@world";
  interface::types::DomainIdType valid_domain_id = "bit.connect";
  interface::types::QuorumType valid_quorum = 1;
  interface::types::JsonType valid_json = R"({"name": "json" })";

  interface::types::AccountIdType invalid_account_id = "hello123";
};

/**
 * @given valid data for account
 * @when account is created via factory
 * @then peer is successfully initialized
 */
TEST_F(AccountTest, ValidAccountInitialization) {
  auto account = factory.createAccount(
      valid_account_id, valid_domain_id, valid_quorum, valid_json);

  account.match(
      [&](const ValueOf<decltype(account)> &v) {
        ASSERT_EQ(v.value->accountId(), valid_account_id);
        ASSERT_EQ(v.value->domainId(), valid_domain_id);
        ASSERT_EQ(v.value->quorum(), valid_quorum);
        ASSERT_EQ(v.value->jsonData(), valid_json);
      },
      [](const ErrorOf<decltype(account)> &e) { FAIL() << e.error; });
}

/**
 * @given invalid data for account
 * @when account is created via factory
 * @then account is not initialized correctly
 */
TEST_F(AccountTest, InvalidAccountInitialization) {
  auto account = factory.createAccount(
      invalid_account_id, valid_domain_id, valid_quorum, valid_json);

  account.match(
      [](const ValueOf<decltype(account)> &v) {
        FAIL() << "Expected error case";
      },
      [](const ErrorOf<decltype(account)> &e) { SUCCEED(); });
}

class AccountAssetTest : public ::testing::Test {
 public:
  interface::types::AccountIdType valid_account_id = "hello@world";
  interface::types::AssetIdType valid_asset_id = "bit#connect";
  interface::Amount valid_amount = interface::Amount("10.00");

  interface::types::AccountIdType invalid_account_id = "hello123";
};

/**
 * @given valid data for account asset
 * @when account asset is created via factory
 * @then account asset is successfully initialized
 */
TEST_F(AccountAssetTest, ValidAccountAssetInitialization) {
  auto account_asset = factory.createAccountAsset(
      valid_account_id, valid_asset_id, valid_amount);

  account_asset.match(
      [&](const ValueOf<decltype(account_asset)> &v) {
        ASSERT_EQ(v.value->accountId(), valid_account_id);
        ASSERT_EQ(v.value->assetId(), valid_asset_id);
        ASSERT_EQ(v.value->balance(), valid_amount);
      },
      [](const ErrorOf<decltype(account_asset)> &e) { FAIL() << e.error; });
}

/**
 * @given invalid data for account asset
 * @when account asset is created via factory
 * @then account asset is not initialized correctly
 */
TEST_F(AccountAssetTest, InvalidAccountAssetInitialization) {
  auto account_asset = factory.createAccountAsset(
      invalid_account_id, valid_asset_id, valid_amount);

  account_asset.match(
      [](const ValueOf<decltype(account_asset)> &v) {
        FAIL() << "Expected error case";
      },
      [](const ErrorOf<decltype(account_asset)> &e) { SUCCEED(); });
}

class AssetTest : public ::testing::Test {
 public:
  interface::types::AssetIdType valid_asset_id = "bit#connect";
  interface::types::DomainIdType valid_domain_id = "iroha.com";
  interface::types::PrecisionType valid_precision = 2;

  interface::types::AssetIdType invalid_asset_id = "bit";
};

/**
 * @given valid data for asset
 * @when asset is created via factory
 * @then asset is successfully initialized
 */
TEST_F(AssetTest, ValidAssetInitialization) {
  auto asset =
      factory.createAsset(valid_asset_id, valid_domain_id, valid_precision);

  asset.match(
      [&](const ValueOf<decltype(asset)> &v) {
        ASSERT_EQ(v.value->assetId(), valid_asset_id);
        ASSERT_EQ(v.value->domainId(), valid_domain_id);
        ASSERT_EQ(v.value->precision(), valid_precision);
      },
      [](const ErrorOf<decltype(asset)> &e) { FAIL() << e.error; });
}

/**
 * @given invalid data for asset
 * @when asset is created via factory
 * @then asset is not initialized correctly
 */
TEST_F(AssetTest, InvalidAssetInitialization) {
  auto asset =
      factory.createAsset(invalid_asset_id, valid_domain_id, valid_precision);

  asset.match(
      [](const ValueOf<decltype(asset)> &v) {
        FAIL() << "Expected error case";
      },
      [](const ErrorOf<decltype(asset)> &e) { SUCCEED(); });
}

class DomainTest : public ::testing::Test {
 public:
  interface::types::DomainIdType valid_domain_id = "iroha.com";
  interface::types::RoleIdType valid_role_id = "admin";

  interface::types::DomainIdType invalid_domain_id = "123irohacom";
};

/**
 * @given valid data for domain
 * @when domain is created via factory
 * @then domain is successfully initialized
 */
TEST_F(DomainTest, ValidDomainInitialization) {
  auto domain =
      factory.createDomain(valid_domain_id, valid_role_id);

  domain.match(
      [&](const ValueOf<decltype(domain)> &v) {
        ASSERT_EQ(v.value->domainId(), valid_domain_id);
        ASSERT_EQ(v.value->defaultRole(), valid_role_id);
      },
      [](const ErrorOf<decltype(domain)> &e) { FAIL() << e.error; });
}

/**
 * @given invalid data for domain
 * @when domain is created via factory
 * @then domain is not initialized correctly
 */
TEST_F(DomainTest, InvalidDomainInitialization) {
  auto domain =
      factory.createDomain(invalid_domain_id, valid_role_id);

  domain.match(
      [](const ValueOf<decltype(domain)> &v) {
        FAIL() << "Expected error case";
      },
      [](const ErrorOf<decltype(domain)> &e) { SUCCEED(); });
}

class SignatureTest : public ::testing::Test {
 public:
  crypto::PublicKey valid_pubkey =
      crypto::DefaultCryptoAlgorithmType::generateKeypair().publicKey();
  crypto::Signed valid_data{"hello"};
  crypto::PublicKey invalid_pubkey{"1234"};
};

/**
 * @given valid data for signature
 * @when signature is created via factory
 * @then signature is successfully initialized
 */
TEST_F(SignatureTest, ValidSignatureInitialization) {
  auto signature =
      factory.createSignature(valid_pubkey, valid_data);

  signature.match(
      [&](const ValueOf<decltype(signature)> &v) {
        ASSERT_EQ(v.value->publicKey().hex(), valid_pubkey.hex());
        ASSERT_EQ(v.value->signedData().hex(), valid_data.hex());
      },
      [](const ErrorOf<decltype(signature)> &e) { FAIL() << e.error; });
}

/**
 * @given invalid data for signature
 * @when signature is created via factory
 * @then signature is not initialized correctly
 */
TEST_F(SignatureTest, InvalidSignatureInitialization) {
  auto signature =
      factory.createSignature(invalid_pubkey, valid_data);

  signature.match(
      [](const ValueOf<decltype(signature)> &v) {
        FAIL() << "Expected error case";
      },
      [](const ErrorOf<decltype(signature)> &e) { SUCCEED(); });
}
