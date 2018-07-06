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

    PostgresWsvCommand::PostgresWsvCommand(pqxx::nontransaction &transaction)
        : transaction_(transaction),
          execute_{makeExecuteResult(transaction_)} {}

    WsvCommandResult PostgresWsvCommand::insertRole(
        const shared_model::interface::types::RoleIdType &role_name) {
      auto result = execute_("INSERT INTO role(role_id) VALUES ("
                             + transaction_.quote(role_name) + ");");

      auto message_gen = [&] {
        return (boost::format("failed to insert role: '%s'") % role_name).str();
      };

      return makeCommandResult(std::move(result), message_gen);
    }

    WsvCommandResult PostgresWsvCommand::insertAccountRole(
        const shared_model::interface::types::AccountIdType &account_id,
        const shared_model::interface::types::RoleIdType &role_name) {
      auto result =
          execute_("INSERT INTO account_has_roles(account_id, role_id) VALUES ("
                   + transaction_.quote(account_id) + ", "
                   + transaction_.quote(role_name) + ");");

      auto message_gen = [&] {
        return (boost::format("failed to insert account role, account: '%s', "
                              "role name: '%s'")
                % account_id % role_name)
            .str();
      };

      return makeCommandResult(std::move(result), message_gen);
    }

    WsvCommandResult PostgresWsvCommand::deleteAccountRole(
        const shared_model::interface::types::AccountIdType &account_id,
        const shared_model::interface::types::RoleIdType &role_name) {
      auto result = execute_("DELETE FROM account_has_roles WHERE account_id="
                             + transaction_.quote(account_id) + "AND role_id="
                             + transaction_.quote(role_name) + ";");
      auto message_gen = [&] {
        return (boost::format(
                    "failed to delete account role, account id: '%s', "
                    "role name: '%s'")
                % account_id % role_name)
            .str();
      };

      return makeCommandResult(std::move(result), message_gen);
    }

    WsvCommandResult PostgresWsvCommand::insertRolePermissions(
        const shared_model::interface::types::RoleIdType &role_id,
        const shared_model::interface::RolePermissionSet &permissions) {
      auto perm_str = permissions.toBitstring();
      auto result = execute_(
          "INSERT INTO role_has_permissions(role_id, permission) VALUES ("
          + transaction_.quote(role_id) + ", " + transaction_.quote(perm_str)
          + " );");

      auto message_gen = [&] {
        // TODO(@l4l) 26/06/18 need to be simplified at IR-1479
        const auto &str =
            shared_model::proto::permissions::toString(permissions);
        const auto perm_debug_str =
            std::accumulate(str.begin(), str.end(), std::string());
        return (boost::format("failed to insert role permissions, role "
                              "id: '%s', permissions: [%s]")
                % role_id % perm_debug_str)
            .str();
      };

      return makeCommandResult(std::move(result), message_gen);
    }

    WsvCommandResult PostgresWsvCommand::insertAccountGrantablePermission(
        const shared_model::interface::types::AccountIdType
            &permittee_account_id,
        const shared_model::interface::types::AccountIdType &account_id,
        shared_model::interface::permissions::Grantable permission) {
      const auto perm_str =
          shared_model::interface::GrantablePermissionSet({permission})
              .toBitstring();
      auto query =
          (boost::format(
               "INSERT INTO account_has_grantable_permissions as "
               "has_perm(permittee_account_id, account_id, permission) VALUES "
               "(%1%, %2%, %3%) ON CONFLICT (permittee_account_id, account_id) "
               // SELECT will end up with a error, if the permission exists
               "DO UPDATE SET permission=(SELECT has_perm.permission | %3% "
               "WHERE (has_perm.permission & %3%) <> %3%);")
           % transaction_.quote(permittee_account_id)
           % transaction_.quote(account_id) % transaction_.quote(perm_str))
              .str();
      auto result = execute_(query);

      auto message_gen = [&] {
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

      return makeCommandResult(std::move(result), message_gen);
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
      auto query =
          (boost::format("UPDATE account_has_grantable_permissions as has_perm "
                         // SELECT will end up with a error, if the permission
                         // doesn't exists
                         "SET permission=(SELECT has_perm.permission & %3% "
                         "WHERE has_perm.permission & %3% = %3%) WHERE "
                         "permittee_account_id=%1% AND account_id=%2%;")
           % transaction_.quote(permittee_account_id)
           % transaction_.quote(account_id) % transaction_.quote(perm_str))
              .str();
      auto result = execute_(query);

      auto message_gen = [&] {
        return (boost::format("failed to delete account grantable permission, "
                              "permittee account id: '%s', "
                              "account id: '%s', "
                              "permission id: '%s'")
                % permittee_account_id
                % account_id
                // TODO(@l4l) 26/06/18 need to be simplified at IR-1479
                % shared_model::proto::permissions::toString(permission))
            .str();
      };

      return makeCommandResult(std::move(result), message_gen);
    }

    WsvCommandResult PostgresWsvCommand::insertAccount(
        const shared_model::interface::Account &account) {
      auto result = execute_(
          "INSERT INTO account(account_id, domain_id, quorum, "
          "data) VALUES ("
          + transaction_.quote(account.accountId()) + ", "
          + transaction_.quote(account.domainId()) + ", "
          + transaction_.quote(account.quorum()) + ", "
          + transaction_.quote(account.jsonData()) + ");");

      auto message_gen = [&] {
        return (boost::format("failed to insert account, "
                              "account id: '%s', "
                              "domain id: '%s', "
                              "quorum: '%d', "
                              "json_data: %s")
                % account.accountId() % account.domainId() % account.quorum()
                % account.jsonData())
            .str();
      };

      return makeCommandResult(std::move(result), message_gen);
    }

    WsvCommandResult PostgresWsvCommand::insertAsset(
        const shared_model::interface::Asset &asset) {
      uint32_t precision = asset.precision();
      auto result = execute_(
          "INSERT INTO asset(asset_id, domain_id, \"precision\", data) "
          "VALUES ("
          + transaction_.quote(asset.assetId()) + ", "
          + transaction_.quote(asset.domainId()) + ", "
          + transaction_.quote(precision) + ", " + /*asset.data*/ "NULL"
          + ");");

      auto message_gen = [&] {
        return (boost::format("failed to insert asset, asset id: '%s', "
                              "domain id: '%s', precision: %d")
                % asset.assetId() % asset.domainId() % precision)
            .str();
      };

      return makeCommandResult(std::move(result), message_gen);
    }

    WsvCommandResult PostgresWsvCommand::upsertAccountAsset(
        const shared_model::interface::AccountAsset &asset) {
      auto result = execute_(
            "INSERT INTO account_has_asset(account_id, asset_id, amount) "
            "VALUES ("
            + transaction_.quote(asset.accountId()) + ", "
            + transaction_.quote(asset.assetId()) + ", "
            + transaction_.quote(asset.balance().toStringRepr())
            + ") ON CONFLICT (account_id, asset_id) DO UPDATE SET "
            "amount = EXCLUDED.amount;");

      auto message_gen = [&] {
        return (boost::format("failed to upsert account, account id: '%s', "
                              "asset id: '%s', balance: %s")
                % asset.accountId() % asset.assetId()
                % asset.balance().toString())
            .str();
      };

      return makeCommandResult(std::move(result), message_gen);
    }

    WsvCommandResult PostgresWsvCommand::insertSignatory(
        const shared_model::interface::types::PubkeyType &signatory) {
      auto result =
          execute_("INSERT INTO signatory(public_key) VALUES ("
                   + transaction_.quote(pqxx::binarystring(
                         signatory.blob().data(), signatory.blob().size()))
                   + ") ON CONFLICT DO NOTHING;");
      auto message_gen = [&] {
        return (boost::format(
                    "failed to insert signatory, signatory hex string: '%s'")
                % signatory.hex())
            .str();
      };
      return makeCommandResult(std::move(result), message_gen);
    }

    WsvCommandResult PostgresWsvCommand::insertAccountSignatory(
        const shared_model::interface::types::AccountIdType &account_id,
        const shared_model::interface::types::PubkeyType &signatory) {
      auto result = execute_(
          "INSERT INTO account_has_signatory(account_id, public_key) VALUES ("
          + transaction_.quote(account_id) + ", "
          + transaction_.quote(pqxx::binarystring(signatory.blob().data(),
                                                  signatory.blob().size()))
          + ");");

      auto message_gen = [&] {
        return (boost::format("failed to insert account signatory, account id: "
                              "'%s', signatory hex string: '%s")
                % account_id % signatory.hex())
            .str();
      };
      return makeCommandResult(std::move(result), message_gen);
    }

    WsvCommandResult PostgresWsvCommand::deleteAccountSignatory(
        const shared_model::interface::types::AccountIdType &account_id,
        const shared_model::interface::types::PubkeyType &signatory) {
      auto result =
          execute_("DELETE FROM account_has_signatory WHERE account_id = "
                   + transaction_.quote(account_id) + " AND public_key = "
                   + transaction_.quote(pqxx::binarystring(
                         signatory.blob().data(), signatory.blob().size()))
                   + ";");

      auto message_gen = [&] {
        return (boost::format("failed to delete account signatory, account id: "
                              "'%s', signatory hex string: '%s'")
                % account_id % signatory.hex())
            .str();
      };
      return makeCommandResult(std::move(result), message_gen);
    }

    WsvCommandResult PostgresWsvCommand::deleteSignatory(
        const shared_model::interface::types::PubkeyType &signatory) {
      pqxx::binarystring public_key(signatory.blob().data(),
                                    signatory.blob().size());
      auto result = execute_("DELETE FROM signatory WHERE public_key = "
                    + transaction_.quote(public_key)
                    + " AND NOT EXISTS (SELECT 1 FROM account_has_signatory "
                        "WHERE public_key = "
                    + transaction_.quote(public_key)
                    + ") AND NOT EXISTS (SELECT 1 FROM peer WHERE public_key = "
                    + transaction_.quote(public_key) + ");");

      auto message_gen = [&] {
        return (boost::format(
                    "failed to delete signatory, signatory hex string: '%s'")
                % signatory.hex())
            .str();
      };
      return makeCommandResult(std::move(result), message_gen);
    }

    WsvCommandResult PostgresWsvCommand::insertPeer(
        const shared_model::interface::Peer &peer) {
      auto result =
          execute_("INSERT INTO peer(public_key, address) VALUES ("
                   + transaction_.quote(pqxx::binarystring(
                         peer.pubkey().blob().data(), peer.pubkey().size()))
                   + ", " + transaction_.quote(peer.address()) + ");");

      auto message_gen = [&] {
        return (boost::format(
                    "failed to insert peer, public key: '%s', address: '%s'")
                % peer.pubkey().hex() % peer.address())
            .str();
      };
      return makeCommandResult(std::move(result), message_gen);
    }

    WsvCommandResult PostgresWsvCommand::deletePeer(
        const shared_model::interface::Peer &peer) {
      auto result = execute_(
          "DELETE FROM peer WHERE public_key = "
          + transaction_.quote(pqxx::binarystring(peer.pubkey().blob().data(),
                                                  peer.pubkey().size()))
          + " AND address = " + transaction_.quote(peer.address()) + ";");
      auto message_gen = [&] {
        return (boost::format(
                    "failed to delete peer, public key: '%s', address: '%s'")
                % peer.pubkey().hex() % peer.address())
            .str();
      };
      return makeCommandResult(std::move(result), message_gen);
    }

    WsvCommandResult PostgresWsvCommand::insertDomain(
        const shared_model::interface::Domain &domain) {
      auto result =
          execute_("INSERT INTO domain(domain_id, default_role) VALUES ("
                   + transaction_.quote(domain.domainId()) + ", "
                   + transaction_.quote(domain.defaultRole()) + ");");

      auto message_gen = [&] {
        return (boost::format("failed to insert domain, domain id: '%s', "
                              "default role: '%s'")
                % domain.domainId() % domain.defaultRole())
            .str();
      };
      return makeCommandResult(std::move(result), message_gen);
    }

    WsvCommandResult PostgresWsvCommand::updateAccount(
        const shared_model::interface::Account &account) {
      auto result = execute_(
            "UPDATE account\n"
            "   SET quorum=" +
            transaction_.quote(account.quorum()) +
            "\n"
            " WHERE account_id=" +
            transaction_.quote(account.accountId()) + ";");

      auto message_gen = [&] {
        return (boost::format(
                    "failed to update account, account id: '%s', quorum: '%s'")
                % account.accountId() % account.quorum())
            .str();
      };
      return makeCommandResult(std::move(result), message_gen);
    }

    WsvCommandResult PostgresWsvCommand::setAccountKV(
        const shared_model::interface::types::AccountIdType &account_id,
        const shared_model::interface::types::AccountIdType &creator_account_id,
        const std::string &key,
        const std::string &val) {
      auto result = execute_(
          "UPDATE account SET data = jsonb_set(CASE WHEN data ?"
          + transaction_.quote(creator_account_id)
          + " THEN data ELSE jsonb_set(data, "
          + transaction_.quote("{" + creator_account_id + "}") + ","
          + transaction_.quote("{}") + ") END,"
          + transaction_.quote("{" + creator_account_id + ", " + key + "}")
          + "," + transaction_.quote("\"" + val + "\"")
          + ") WHERE account_id=" + transaction_.quote(account_id) + ";");

      auto message_gen = [&] {
        return (boost::format(
                    "failed to set account key-value, account id: '%s', "
                    "creator account id: '%s',\n key: '%s', value: '%s'")
                % account_id % creator_account_id % key % val)
            .str();
      };
      return makeCommandResult(std::move(result), message_gen);
    }
  }  // namespace ametsuchi
}  // namespace iroha
