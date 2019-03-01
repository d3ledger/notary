package jp.co.soramitsu.bootstrap.genesis.d3

import iroha.protocol.Primitive
import jp.co.soramitsu.iroha.java.TransactionBuilder
import jp.co.soramitsu.bootstrap.dto.AccountPrototype
import jp.co.soramitsu.bootstrap.dto.PassiveAccountPrototype

object D3TestContext {
    val d3neededAccounts = listOf<AccountPrototype>(
        AccountPrototype("notary", "notary", listOf("notary")),
        AccountPrototype(
            "registration_service",
            "notary",
            listOf("registration_service", "client")
        ),
        AccountPrototype(
            "eth_registration_service",
            "notary",
            listOf("registration_service", "client", "relay_deployer", "whitelist_setter")
        ),
        AccountPrototype(
            "btc_registration_service",
            "notary",
            listOf("registration_service", "client")
        ),
        AccountPrototype(
            "mst_btc_registration_service",
            "notary",
            listOf("registration_service", "client")
        ),
        AccountPrototype(
            "eth_token_storage_service",
            "notary",
            listOf("eth_token_list_storage")
        ),
        AccountPrototype(
            "withdrawal",
            "notary",
            listOf("withdrawal")
        ),
        AccountPrototype(
            "btc_fee_rate",
            "notary",
            listOf("btc_fee_rate_setter")
        ),
        AccountPrototype(
            "btc_withdrawal_service",
            "notary",
            listOf("withdrawal", "rollback")
        ),
        AccountPrototype(
            "btc_sign_collector",
            "notary",
            listOf("signature_collector")
        ),
        AccountPrototype(
            "test",
            "notary",
            listOf("tester", "registration_service", "client")
        ),
        AccountPrototype("vacuumer", "notary", listOf("vacuumer")),
        PassiveAccountPrototype(
            "notaries",
            "notary",
            listOf("notary_list_holder")
        ),
        PassiveAccountPrototype(
            "btc_change_addresses",
            "notary",
            details = hashMapOf(Pair("some_notary", "http://localhost:20000"))
        ),
        PassiveAccountPrototype("gen_btc_pk_trigger", "notary"),
        AccountPrototype("admin", "notary", listOf("admin")),
        AccountPrototype("sora", "sora", listOf("sora"))
    )


    fun createNotaryRole(builder: TransactionBuilder) {
        builder.createRole(
            "notary",
            listOf(
                Primitive.RolePermission.can_get_all_acc_ast,
                Primitive.RolePermission.can_get_all_accounts,
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

    fun createRegistrationServiceRole(builder: TransactionBuilder) {
        builder.createRole(
            "registration_service",
            listOf(
                Primitive.RolePermission.can_create_account,
                Primitive.RolePermission.can_append_role,
                Primitive.RolePermission.can_set_detail,
                Primitive.RolePermission.can_get_all_accounts,
                Primitive.RolePermission.can_get_domain_accounts,
                Primitive.RolePermission.can_get_all_txs,
                Primitive.RolePermission.can_get_blocks,
                Primitive.RolePermission.can_set_quorum
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
                Primitive.RolePermission.can_remove_signatory
            )
        )
    }

    fun createWithdrawalRole(builder: TransactionBuilder) {
        builder.createRole(
            "withdrawal",
            listOf(
                Primitive.RolePermission.can_get_all_accounts,
                Primitive.RolePermission.can_get_blocks,
                Primitive.RolePermission.can_read_assets,
                Primitive.RolePermission.can_receive,
                Primitive.RolePermission.can_get_all_txs
            )

        )
    }

    fun createSignatureCollectorRole(builder: TransactionBuilder) {
        builder.createRole(
            "signature_collector",
            listOf(
                Primitive.RolePermission.can_create_account,
                Primitive.RolePermission.can_set_detail,
                Primitive.RolePermission.can_get_all_accounts
            )

        )
    }

    fun createVacuumerRole(builder: TransactionBuilder) {
        builder.createRole(
            "Vacuumer",
            listOf(
                Primitive.RolePermission.can_get_domain_accounts,
                Primitive.RolePermission.can_read_assets
            )

        )
    }

    fun createNoneRole(builder: TransactionBuilder) {
        builder.createRole(
            "none",
            listOf()
        )
    }

    fun createTesterRole(builder: TransactionBuilder) {
        builder.createRole(
            "tester",
            listOf(
                Primitive.RolePermission.can_create_account,
                Primitive.RolePermission.can_set_detail,
                Primitive.RolePermission.can_create_asset,
                Primitive.RolePermission.can_transfer,
                Primitive.RolePermission.can_receive,
                Primitive.RolePermission.can_add_asset_qty,
                Primitive.RolePermission.can_subtract_asset_qty,
                Primitive.RolePermission.can_create_domain,
                Primitive.RolePermission.can_grant_can_add_my_signatory,
                Primitive.RolePermission.can_grant_can_remove_my_signatory,
                Primitive.RolePermission.can_grant_can_set_my_quorum,
                Primitive.RolePermission.can_grant_can_transfer_my_assets,
                Primitive.RolePermission.can_add_peer,
                Primitive.RolePermission.can_append_role,
                Primitive.RolePermission.can_create_role,
                Primitive.RolePermission.can_detach_role,
                Primitive.RolePermission.can_add_signatory,
                Primitive.RolePermission.can_remove_signatory,
                Primitive.RolePermission.can_set_quorum,
                Primitive.RolePermission.can_get_all_acc_detail,
                Primitive.RolePermission.can_get_all_accounts,
                Primitive.RolePermission.can_get_all_acc_ast,
                Primitive.RolePermission.can_get_blocks,
                Primitive.RolePermission.can_get_roles,
                Primitive.RolePermission.can_get_all_signatories,
                Primitive.RolePermission.can_get_domain_accounts,
                Primitive.RolePermission.can_get_all_txs,
                Primitive.RolePermission.can_get_domain_acc_detail,
                Primitive.RolePermission.can_read_assets
            )
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
                Primitive.RolePermission.can_transfer
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

    fun createSoraRole(builder: TransactionBuilder) {
        builder.createRole(
            "notary_list_holder",
            listOf(
                Primitive.RolePermission.can_get_my_acc_ast,
                Primitive.RolePermission.can_transfer,
                Primitive.RolePermission.can_receive
            )
        )
    }

    fun createSoraClientRole(builder: TransactionBuilder) {
        builder.createRole(
            "notary_list_holder",
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
                Primitive.RolePermission.can_remove_signatory
            )
        )
    }

    fun createBtcFeeRateSetterRole(builder: TransactionBuilder) {
        builder.createRole(
            "notary_list_holder",
            listOf(
                Primitive.RolePermission.can_set_detail,
                Primitive.RolePermission.can_get_all_accounts
            )
        )
    }
}
