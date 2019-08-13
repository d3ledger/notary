/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap.genesis.d3

import iroha.protocol.Primitive
import jp.co.soramitsu.bootstrap.changelog.ChangelogInterface
import jp.co.soramitsu.bootstrap.dto.AccountPrototype
import jp.co.soramitsu.bootstrap.dto.PassiveAccountPrototype
import jp.co.soramitsu.bootstrap.dto.PeersCountDependentAccountPrototype
import jp.co.soramitsu.iroha.java.TransactionBuilder

object D3TestContext {

    val d3neededAccounts = listOf(
        PassiveAccountPrototype(
            "changelog_history",
            "bootstrap"
        ),
        AccountPrototype(
            "rmq",
            "notary",
            listOf("rmq")
        ),
        AccountPrototype(
            "data_collector",
            "notary",
            listOf("data_collector")
        ),
        AccountPrototype(
            "btc_consensus_collector",
            "notary",
            listOf("consensus_collector")
        ),
        PeersCountDependentAccountPrototype(
            "notary",
            "notary",
            listOf("notary")
        ),
        PeersCountDependentAccountPrototype(
            ChangelogInterface.superuserAccount,
            ChangelogInterface.superuserDomain,
            listOf(ChangelogInterface.superuserAccount)
        ),
        AccountPrototype(
            "eth_registration_service",
            "notary",
            listOf("registration_service", "client", "relay_deployer")
        ),
        AccountPrototype(
            "eth_token_service",
            "notary",
            listOf("eth_token_list_storage")
        ),
        PassiveAccountPrototype(
            "eth_anchored_token_service",
            "notary"
        ),
        PassiveAccountPrototype(
            "iroha_anchored_token_service",
            "notary"
        ),
        AccountPrototype(
            "btc_registration_service",
            "notary",
            listOf("registration_service", "client")
        ),
        PeersCountDependentAccountPrototype(
            "mst_btc_registration_service",
            "notary",
            listOf("registration_service", "client", "notary")
        ),
        AccountPrototype(
            "withdrawal",
            "notary",
            listOf("withdrawal")
        ),
        PeersCountDependentAccountPrototype(
            "btc_withdrawal_service",
            "notary",
            listOf("withdrawal", "rollback", "notary")
        ),
        AccountPrototype(
            "btc_sign_collector",
            "notary",
            listOf("signature_collector")
        ),
        AccountPrototype("vacuumer", "notary", listOf("vacuumer")),
        PassiveAccountPrototype(
            "notaries",
            "notary",
            listOf("notary_list_holder"),
            details = hashMapOf(Pair("some_notary", "http://localhost:20000"))
        ),
        PassiveAccountPrototype(
            "btc_change_addresses",
            "notary"
        ),
        PassiveAccountPrototype(
            "btc_tx_storage",
            "notary"
        ),
        PassiveAccountPrototype(
            "btc_utxo_storage_v2",
            "notary"
        ),
        PassiveAccountPrototype("gen_btc_pk_trigger", "notary"),
        AccountPrototype("admin", "notary", listOf("admin")),
        AccountPrototype("exchanger", "notary", listOf("exchange")),
        AccountPrototype("broadcast", "notary", listOf("broadcast"))
    )

    fun createDataCollectorRole(builder: TransactionBuilder) {
        builder.createRole(
            "data_collector",
            listOf(Primitive.RolePermission.can_get_blocks)
        )
    }


    fun createNotaryRole(builder: TransactionBuilder) {
        builder.createRole(
            "notary",
            listOf(
                Primitive.RolePermission.can_get_all_acc_ast,
                Primitive.RolePermission.can_get_all_accounts,
                Primitive.RolePermission.can_get_all_acc_detail,
                Primitive.RolePermission.can_create_asset,
                Primitive.RolePermission.can_add_asset_qty,
                Primitive.RolePermission.can_transfer,
                Primitive.RolePermission.can_set_detail,
                Primitive.RolePermission.can_get_all_txs,
                Primitive.RolePermission.can_receive,
                Primitive.RolePermission.can_get_blocks,
                Primitive.RolePermission.can_read_assets,
                Primitive.RolePermission.can_add_signatory,
                Primitive.RolePermission.can_set_quorum,
                Primitive.RolePermission.can_grant_can_set_my_quorum,
                Primitive.RolePermission.can_grant_can_add_my_signatory,
                Primitive.RolePermission.can_grant_can_transfer_my_assets
            )
        )
    }

    fun createRelayDeployerRole(builder: TransactionBuilder) {
        builder.createRole(
            "relay_deployer",
            listOf(
                Primitive.RolePermission.can_set_detail,
                Primitive.RolePermission.can_get_domain_accounts
            )
        )
    }

    fun createEthTokenListStorageRole(builder: TransactionBuilder) {
        builder.createRole(
            "eth_token_list_storage",
            listOf(
                Primitive.RolePermission.can_set_detail,
                Primitive.RolePermission.can_create_asset
            )
        )
    }

    fun createRmqRole(builder: TransactionBuilder) {
        builder.createRole(
            "rmq",
            listOf(
                Primitive.RolePermission.can_get_blocks
            )
        )
    }

    fun createBtcConsensusRole(builder: TransactionBuilder) {
        builder.createRole(
            "consensus_collector",
            listOf(
                Primitive.RolePermission.can_create_account,
                Primitive.RolePermission.can_set_detail,
                Primitive.RolePermission.can_get_all_accounts
            )
        )
    }

    fun createRegistrationServiceRole(builder: TransactionBuilder) {
        builder.createRole(
            "registration_service",
            listOf(
                Primitive.RolePermission.can_create_account,
                Primitive.RolePermission.can_append_role,
                Primitive.RolePermission.can_set_detail,
                Primitive.RolePermission.can_get_all_accounts,
                Primitive.RolePermission.can_get_domain_accounts,
                Primitive.RolePermission.can_get_all_acc_detail,
                Primitive.RolePermission.can_get_all_txs,
                Primitive.RolePermission.can_get_blocks,
                Primitive.RolePermission.can_set_quorum,
                Primitive.RolePermission.can_grant_can_set_my_quorum,
                Primitive.RolePermission.can_grant_can_add_my_signatory
            )
        )
    }

    fun createClientRole(builder: TransactionBuilder) {
        builder.createRole(
            "client",
            listOf(
                Primitive.RolePermission.can_get_my_account,
                Primitive.RolePermission.can_get_my_acc_ast,
                Primitive.RolePermission.can_get_my_acc_ast_txs,
                Primitive.RolePermission.can_get_my_acc_txs,
                Primitive.RolePermission.can_get_my_txs,
                Primitive.RolePermission.can_transfer,
                Primitive.RolePermission.can_receive,
                Primitive.RolePermission.can_set_quorum,
                Primitive.RolePermission.can_add_signatory,
                Primitive.RolePermission.can_get_my_signatories,
                Primitive.RolePermission.can_remove_signatory,
                Primitive.RolePermission.can_grant_can_set_my_quorum,
                Primitive.RolePermission.can_grant_can_add_my_signatory,
                Primitive.RolePermission.can_grant_can_remove_my_signatory
            )
        )
    }

    fun createWithdrawalRole(builder: TransactionBuilder) {
        builder.createRole(
            "withdrawal",
            listOf(
                Primitive.RolePermission.can_get_all_accounts,
                Primitive.RolePermission.can_get_all_acc_detail,
                Primitive.RolePermission.can_get_blocks,
                Primitive.RolePermission.can_read_assets,
                Primitive.RolePermission.can_receive,
                Primitive.RolePermission.can_transfer,
                Primitive.RolePermission.can_get_all_txs,
                Primitive.RolePermission.can_set_detail,
                Primitive.RolePermission.can_set_quorum,
                Primitive.RolePermission.can_add_signatory,
                Primitive.RolePermission.can_subtract_asset_qty
            )

        )
    }

    fun createSignatureCollectorRole(builder: TransactionBuilder) {
        builder.createRole(
            "signature_collector",
            listOf(
                Primitive.RolePermission.can_create_account,
                Primitive.RolePermission.can_set_detail,
                Primitive.RolePermission.can_get_all_accounts,
                Primitive.RolePermission.can_get_all_acc_detail
            )

        )
    }

    fun createVacuumerRole(builder: TransactionBuilder) {
        builder.createRole(
            "vacuumer",
            listOf(
                Primitive.RolePermission.can_get_domain_accounts,
                Primitive.RolePermission.can_read_assets,
                Primitive.RolePermission.can_get_all_acc_detail
            )

        )
    }

    fun createNoneRole(builder: TransactionBuilder) {
        builder.createRole(
            "none",
            listOf()
        )
    }

    fun createWhiteListSetterRole(builder: TransactionBuilder) {
        builder.createRole(
            "whitelist_setter",
            listOf(
                Primitive.RolePermission.can_set_detail
            )
        )
    }

    fun createRollBackRole(builder: TransactionBuilder) {
        builder.createRole(
            "rollback",
            listOf(
                Primitive.RolePermission.can_transfer,
                Primitive.RolePermission.can_set_quorum,
                Primitive.RolePermission.can_set_detail,
                Primitive.RolePermission.can_grant_can_set_my_quorum,
                Primitive.RolePermission.can_grant_can_add_my_signatory
            )
        )
    }

    fun createNotaryListHolderRole(builder: TransactionBuilder) {
        builder.createRole(
            "notary_list_holder",
            listOf(
                Primitive.RolePermission.can_set_detail
            )
        )
    }

    fun createAdminRole(builder: TransactionBuilder) {
        builder.createRole(
            "admin",
            listOf(
                Primitive.RolePermission.can_get_all_accounts,
                Primitive.RolePermission.can_create_account,
                Primitive.RolePermission.can_set_detail,
                Primitive.RolePermission.can_get_all_acc_detail,
                Primitive.RolePermission.can_get_all_acc_ast,
                Primitive.RolePermission.can_get_all_acc_ast_txs,
                Primitive.RolePermission.can_get_all_acc_txs,
                Primitive.RolePermission.can_read_assets,
                Primitive.RolePermission.can_get_blocks,
                Primitive.RolePermission.can_get_all_signatories,
                Primitive.RolePermission.can_get_all_txs,
                Primitive.RolePermission.can_transfer
            )
        )
    }

    fun createBrvsRole(builder: TransactionBuilder) {
        builder.createRole(
            "brvs",
            listOf(
                Primitive.RolePermission.can_add_signatory,
                Primitive.RolePermission.can_remove_signatory,
                Primitive.RolePermission.can_get_all_signatories,
                Primitive.RolePermission.can_get_all_accounts,
                Primitive.RolePermission.can_get_all_txs,
                Primitive.RolePermission.can_get_blocks,
                Primitive.RolePermission.can_get_all_acc_detail,
                Primitive.RolePermission.can_set_quorum,
                Primitive.RolePermission.can_set_detail
            )
        )
    }

    fun createExchangeRole(builder: TransactionBuilder) {
        builder.createRole(
            "exchange",
            listOf(
                Primitive.RolePermission.can_transfer,
                Primitive.RolePermission.can_receive,
                Primitive.RolePermission.can_read_assets,
                Primitive.RolePermission.can_get_my_acc_ast
            )
        )
    }

    fun createBillingRole(builder: TransactionBuilder) {
        builder.createRole(
            "billing",
            listOf(
                Primitive.RolePermission.can_transfer,
                Primitive.RolePermission.can_receive
            )
        )
    }

    fun createBroadcastRole(builder: TransactionBuilder) {
        builder.createRole(
            "broadcast",
            listOf(
                Primitive.RolePermission.can_set_detail
            )
        )
    }

    fun createSuperuserRole(builder: TransactionBuilder) {
        builder.createRole(
            ChangelogInterface.superuserAccount,
            listOf(
                // all permissions
                Primitive.RolePermission.can_append_role,
                Primitive.RolePermission.can_create_role,
                Primitive.RolePermission.can_detach_role,
                Primitive.RolePermission.can_add_asset_qty,
                Primitive.RolePermission.can_subtract_asset_qty,
                Primitive.RolePermission.can_add_peer,
                Primitive.RolePermission.can_remove_peer,
                Primitive.RolePermission.can_add_signatory,
                Primitive.RolePermission.can_remove_signatory,
                Primitive.RolePermission.can_set_quorum,
                Primitive.RolePermission.can_create_account,
                Primitive.RolePermission.can_set_detail,
                Primitive.RolePermission.can_create_asset,
                Primitive.RolePermission.can_transfer,
                Primitive.RolePermission.can_receive,
                Primitive.RolePermission.can_create_domain,
                Primitive.RolePermission.can_add_domain_asset_qty,
                Primitive.RolePermission.can_subtract_domain_asset_qty,
                // all query permissions
                Primitive.RolePermission.can_read_assets,
                Primitive.RolePermission.can_get_roles,
                Primitive.RolePermission.can_get_my_account,
                Primitive.RolePermission.can_get_all_accounts,
                Primitive.RolePermission.can_get_domain_accounts,
                Primitive.RolePermission.can_get_my_signatories,
                Primitive.RolePermission.can_get_all_signatories,
                Primitive.RolePermission.can_get_domain_signatories,
                Primitive.RolePermission.can_get_my_acc_ast,
                Primitive.RolePermission.can_get_all_acc_ast,
                Primitive.RolePermission.can_get_domain_acc_txs,
                Primitive.RolePermission.can_get_my_acc_detail,
                Primitive.RolePermission.can_get_all_acc_detail,
                Primitive.RolePermission.can_get_domain_acc_detail,
                Primitive.RolePermission.can_get_my_acc_txs,
                Primitive.RolePermission.can_get_all_acc_txs,
                Primitive.RolePermission.can_get_domain_acc_ast,
                Primitive.RolePermission.can_get_my_acc_ast_txs,
                Primitive.RolePermission.can_get_all_acc_ast_txs,
                Primitive.RolePermission.can_get_domain_acc_ast_txs,
                Primitive.RolePermission.can_get_my_txs,
                Primitive.RolePermission.can_get_all_txs,
                Primitive.RolePermission.can_get_blocks,
                Primitive.RolePermission.can_get_peers,
                // all grant permissions
                Primitive.RolePermission.can_grant_can_set_my_quorum,
                Primitive.RolePermission.can_grant_can_add_my_signatory,
                Primitive.RolePermission.can_grant_can_remove_my_signatory,
                Primitive.RolePermission.can_grant_can_transfer_my_assets,
                Primitive.RolePermission.can_grant_can_set_my_account_detail
            )
        )
    }
}
