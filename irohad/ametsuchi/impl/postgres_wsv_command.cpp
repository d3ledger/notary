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

#include "ametsuchi/impl/postgres_wsv_command.hpp"

#include <boost/format.hpp>
#include "backend/protobuf/permissions.hpp"

namespace iroha {
  namespace ametsuchi {

    template <typename Function>
    WsvCommandResult execute(soci::statement &st, Function &&error) {
      st.define_and_bind();
      try {
        st.execute(true);
        return {};
      } catch (const std::exception &e) {
        return expected::makeError(error());
      }
    }

    PostgresWsvCommand::PostgresWsvCommand(soci::session &sql) : sql_(sql) {}

    WsvCommandResult PostgresWsvCommand::insertRole(
        const shared_model::interface::types::RoleIdType &role_name) {
      soci::statement st = sql_.prepare
          << "INSERT INTO role(role_id) VALUES (:role_id)";
      st.exchange(soci::use(role_name));
      auto msg = [&] {
        return (boost::format("failed to insert role: '%s'") % role_name).str();
      };
      return execute(st, msg);
    }

    WsvCommandResult PostgresWsvCommand::insertAccountRole(
        const shared_model::interface::types::AccountIdType &account_id,
        const shared_model::interface::types::RoleIdType &role_name) {
      soci::statement st = sql_.prepare
          << "INSERT INTO account_has_roles(account_id, role_id) VALUES "
             "(:account_id, :role_id)";
      st.exchange(soci::use(account_id));
      st.exchange(soci::use(role_name));

      auto msg = [&] {
        return (boost::format("failed to insert account role, account: '%s', "
                              "role name: '%s'")
                % account_id % role_name)
            .str();
      };
      return execute(st, msg);
    }

    WsvCommandResult PostgresWsvCommand::deleteAccountRole(
        const shared_model::interface::types::AccountIdType &account_id,
        const shared_model::interface::types::RoleIdType &role_name) {
      soci::statement st = sql_.prepare
          << "DELETE FROM account_has_roles WHERE account_id=:account_id "
             "AND role_id=:role_id";
      st.exchange(soci::use(account_id));
      st.exchange(soci::use(role_name));

      auto msg = [&] {
        return (boost::format(
                    "failed to delete account role, account id: '%s', "
                    "role name: '%s'")
                % account_id % role_name)
            .str();
      };
      return execute(st, msg);
    }

    WsvCommandResult PostgresWsvCommand::insertRolePermissions(
        const shared_model::interface::types::RoleIdType &role_id,
        const shared_model::interface::RolePermissionSet &permissions) {
      auto perm_str = permissions.toBitstring();
      soci::statement st = sql_.prepare
          << "INSERT INTO role_has_permissions(role_id, permission) VALUES "
             "(:id, :perm)";
      st.exchange(soci::use(role_id));
      st.exchange(soci::use(perm_str));

      auto msg = [&] {
        const auto &str =
            shared_model::proto::permissions::toString(permissions);
        std::string perm_debug_str = std::accumulate(
            str.begin(),
            str.end(),
            std::string(),
            [](auto acc, const auto &elem) { return acc + " " + elem; });
        return (boost::format("failed to insert role permissions, role "
                              "id: '%s', permissions: [%s]")
                % role_id % perm_debug_str)
            .str();
      };
      return execute(st, msg);
    }

    WsvCommandResult PostgresWsvCommand::insertAccountGrantablePermission(
        const shared_model::interface::types::AccountIdType
            &permittee_account_id,
        const shared_model::interface::types::AccountIdType &account_id,
        shared_model::interface::permissions::Grantable permission) {
      const auto perm_str =
          shared_model::interface::GrantablePermissionSet({permission})
              .toBitstring();
      soci::statement st = sql_.prepare
          << "INSERT INTO account_has_grantable_permissions as "
             "has_perm(permittee_account_id, account_id, permission) VALUES "
             "(:permittee_account_id, :account_id, :perm) ON CONFLICT "
             "(permittee_account_id, account_id) DO UPDATE SET "
             // SELECT will end up with a error, if the permission exists
             "permission=(SELECT has_perm.permission | :perm WHERE "
             "(has_perm.permission & :perm) <> :perm);";
      st.exchange(soci::use(permittee_account_id, "permittee_account_id"));
      st.exchange(soci::use(account_id, "account_id"));
      st.exchange(soci::use(perm_str, "perm"));

      auto msg = [&] {
        return (boost::format("failed to insert account grantable permission, "
                              "permittee account id: '%s', "
                              "account id: '%s', "
                              "permission: '%s'")
                % permittee_account_id
                % account_id
                // TODO(@l4l) 26/06/18 need to be simplified at IR-1479
                % shared_model::proto::permissions::toString(permission))
            .str();
      };
      return execute(st, msg);
    }

    WsvCommandResult PostgresWsvCommand::deleteAccountGrantablePermission(
        const shared_model::interface::types::AccountIdType
            &permittee_account_id,
        const shared_model::interface::types::AccountIdType &account_id,
        shared_model::interface::permissions::Grantable permission) {
      const auto perm_str = shared_model::interface::GrantablePermissionSet()
                                .set()
                                .unset(permission)
                                .toBitstring();
      soci::statement st = sql_.prepare
          << "UPDATE account_has_grantable_permissions as has_perm SET "
             // SELECT will end up with a error, if the permission doesn't
             // exists
             "permission=(SELECT has_perm.permission & :perm WHERE "
             "has_perm.permission & :perm = :perm) WHERE "
             "permittee_account_id=:permittee_account_id AND "
             "account_id=:account_id;";

      st.exchange(soci::use(permittee_account_id, "permittee_account_id"));
      st.exchange(soci::use(account_id, "account_id"));
      st.exchange(soci::use(perm_str, "perm"));

      auto msg = [&] {
        return (boost::format("failed to delete account grantable permission, "
                              "permittee account id: '%s', "
                              "account id: '%s', "
                              "permission id: '%s'")
                % permittee_account_id % account_id
                % shared_model::proto::permissions::toString(permission))
            .str();
      };
      return execute(st, msg);
    }

    WsvCommandResult PostgresWsvCommand::insertAccount(
        const shared_model::interface::Account &account) {
      soci::statement st = sql_.prepare
          << "INSERT INTO account(account_id, domain_id, quorum,"
             "data) VALUES (:id, :domain_id, :quorum, :data)";
      uint32_t quorum = account.quorum();
      st.exchange(soci::use(account.accountId()));
      st.exchange(soci::use(account.domainId()));
      st.exchange(soci::use(quorum));
      st.exchange(soci::use(account.jsonData()));

      auto msg = [&] {
        return (boost::format("failed to insert account, "
                              "account id: '%s', "
                              "domain id: '%s', "
                              "quorum: '%d', "
                              "json_data: %s")
                % account.accountId() % account.domainId() % account.quorum()
                % account.jsonData())
            .str();
      };
      return execute(st, msg);
    }

    WsvCommandResult PostgresWsvCommand::insertAsset(
        const shared_model::interface::Asset &asset) {
      auto precision = asset.precision();
      soci::statement st = sql_.prepare
          << "INSERT INTO asset(asset_id, domain_id, \"precision\", data) "
             "VALUES (:id, :domain_id, :precision, NULL)";
      st.exchange(soci::use(asset.assetId()));
      st.exchange(soci::use(asset.domainId()));
      st.exchange(soci::use(precision));

      auto msg = [&] {
        return (boost::format("failed to insert asset, asset id: '%s', "
                              "domain id: '%s', precision: %d")
                % asset.assetId() % asset.domainId() % asset.precision())
            .str();
      };
      return execute(st, msg);
    }

    WsvCommandResult PostgresWsvCommand::upsertAccountAsset(
        const shared_model::interface::AccountAsset &asset) {
      auto balance = asset.balance().toStringRepr();
      soci::statement st = sql_.prepare
          << "INSERT INTO account_has_asset(account_id, asset_id, amount) "
             "VALUES (:account_id, :asset_id, :amount) ON CONFLICT "
             "(account_id, asset_id) DO UPDATE SET "
             "amount = EXCLUDED.amount";

      st.exchange(soci::use(asset.accountId()));
      st.exchange(soci::use(asset.assetId()));
      st.exchange(soci::use(balance));

      auto msg = [&] {
        return (boost::format("failed to upsert account, account id: '%s', "
                              "asset id: '%s', balance: %s")
                % asset.accountId() % asset.assetId()
                % asset.balance().toString())
            .str();
      };
      return execute(st, msg);
    }

    WsvCommandResult PostgresWsvCommand::insertSignatory(
        const shared_model::interface::types::PubkeyType &signatory) {
      soci::statement st = sql_.prepare
          << "INSERT INTO signatory(public_key) VALUES (:pk) ON CONFLICT DO "
             "NOTHING;";
      st.exchange(soci::use(signatory.hex()));

      auto msg = [&] {
        return (boost::format(
                    "failed to insert signatory, signatory hex string: '%s'")
                % signatory.hex())
            .str();
      };
      return execute(st, msg);
    }

    WsvCommandResult PostgresWsvCommand::insertAccountSignatory(
        const shared_model::interface::types::AccountIdType &account_id,
        const shared_model::interface::types::PubkeyType &signatory) {
      soci::statement st = sql_.prepare
          << "INSERT INTO account_has_signatory(account_id, public_key) "
             "VALUES (:account_id, :pk)";
      st.exchange(soci::use(account_id));
      st.exchange(soci::use(signatory.hex()));

      auto msg = [&] {
        return (boost::format("failed to insert account signatory, account id: "
                              "'%s', signatory hex string: '%s")
                % account_id % signatory.hex())
            .str();
      };
      return execute(st, msg);
    }

    WsvCommandResult PostgresWsvCommand::deleteAccountSignatory(
        const shared_model::interface::types::AccountIdType &account_id,
        const shared_model::interface::types::PubkeyType &signatory) {
      soci::statement st = sql_.prepare
          << "DELETE FROM account_has_signatory WHERE account_id = "
             ":account_id AND public_key = :pk";
      st.exchange(soci::use(account_id));
      st.exchange(soci::use(signatory.hex()));

      auto msg = [&] {
        return (boost::format("failed to delete account signatory, account id: "
                              "'%s', signatory hex string: '%s'")
                % account_id % signatory.hex())
            .str();
      };
      return execute(st, msg);
    }

    WsvCommandResult PostgresWsvCommand::deleteSignatory(
        const shared_model::interface::types::PubkeyType &signatory) {
      soci::statement st = sql_.prepare
          << "DELETE FROM signatory WHERE public_key = :pk AND NOT EXISTS "
             "(SELECT 1 FROM account_has_signatory "
             "WHERE public_key = :pk) AND NOT EXISTS (SELECT 1 FROM peer "
             "WHERE public_key = :pk)";
      st.exchange(soci::use(signatory.hex(), "pk"));

      auto msg = [&] {
        return (boost::format(
                    "failed to delete signatory, signatory hex string: '%s'")
                % signatory.hex())
            .str();
      };
      return execute(st, msg);
    }

    WsvCommandResult PostgresWsvCommand::insertPeer(
        const shared_model::interface::Peer &peer) {
      soci::statement st = sql_.prepare
          << "INSERT INTO peer(public_key, address) VALUES (:pk, :address)";
      st.exchange(soci::use(peer.pubkey().hex()));
      st.exchange(soci::use(peer.address()));

      auto msg = [&] {
        return (boost::format(
                    "failed to insert peer, public key: '%s', address: '%s'")
                % peer.pubkey().hex() % peer.address())
            .str();
      };
      return execute(st, msg);
    }

    WsvCommandResult PostgresWsvCommand::deletePeer(
        const shared_model::interface::Peer &peer) {
      soci::statement st = sql_.prepare
          << "DELETE FROM peer WHERE public_key = :pk AND address = :address";
      st.exchange(soci::use(peer.pubkey().hex()));
      st.exchange(soci::use(peer.address()));

      auto msg = [&] {
        return (boost::format(
                    "failed to delete peer, public key: '%s', address: '%s'")
                % peer.pubkey().hex() % peer.address())
            .str();
      };
      return execute(st, msg);
    }

    WsvCommandResult PostgresWsvCommand::insertDomain(
        const shared_model::interface::Domain &domain) {
      soci::statement st = sql_.prepare
          << "INSERT INTO domain(domain_id, default_role) VALUES (:id, "
             ":role)";
      st.exchange(soci::use(domain.domainId()));
      st.exchange(soci::use(domain.defaultRole()));

      auto msg = [&] {
        return (boost::format("failed to insert domain, domain id: '%s', "
                              "default role: '%s'")
                % domain.domainId() % domain.defaultRole())
            .str();
      };
      return execute(st, msg);
    }

    WsvCommandResult PostgresWsvCommand::updateAccount(
        const shared_model::interface::Account &account) {
      soci::statement st = sql_.prepare
          << "UPDATE account SET quorum=:quorum WHERE account_id=:account_id";
      uint32_t quorum = account.quorum();
      st.exchange(soci::use(quorum));
      st.exchange(soci::use(account.accountId()));

      auto msg = [&] {
        return (boost::format(
                    "failed to update account, account id: '%s', quorum: '%s'")
                % account.accountId() % account.quorum())
            .str();
      };
      return execute(st, msg);
    }

    WsvCommandResult PostgresWsvCommand::setAccountKV(
        const shared_model::interface::types::AccountIdType &account_id,
        const shared_model::interface::types::AccountIdType &creator_account_id,
        const std::string &key,
        const std::string &val) {
      soci::statement st = sql_.prepare
          << "UPDATE account SET data = jsonb_set("
             "CASE WHEN data ?:creator_account_id THEN data ELSE "
             "jsonb_set(data, :json, :empty_json) END, "
             " :filled_json, :val) WHERE account_id=:account_id";
      std::string json = "{" + creator_account_id + "}";
      std::string empty_json = "{}";
      std::string filled_json = "{" + creator_account_id + ", " + key + "}";
      std::string value = "\"" + val + "\"";
      st.exchange(soci::use(creator_account_id));
      st.exchange(soci::use(json));
      st.exchange(soci::use(empty_json));
      st.exchange(soci::use(filled_json));
      st.exchange(soci::use(value));
      st.exchange(soci::use(account_id));

      auto msg = [&] {
        return (boost::format(
                    "failed to set account key-value, account id: '%s', "
                    "creator account id: '%s',\n key: '%s', value: '%s'")
                % account_id % creator_account_id % key % val)
            .str();
      };

      return execute(st, msg);
    }
  }  // namespace ametsuchi
}  // namespace iroha
