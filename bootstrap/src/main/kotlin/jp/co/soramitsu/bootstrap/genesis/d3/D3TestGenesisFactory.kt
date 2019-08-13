/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap.genesis.d3

import com.google.protobuf.util.JsonFormat
import jp.co.soramitsu.bootstrap.dto.AccountPrototype
import jp.co.soramitsu.bootstrap.dto.AccountPublicInfo
import jp.co.soramitsu.bootstrap.dto.AccountType
import jp.co.soramitsu.bootstrap.dto.Peer
import jp.co.soramitsu.bootstrap.exceptions.AccountException
import jp.co.soramitsu.bootstrap.genesis.*
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.TransactionBuilder
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import java.util.*

class D3TestGenesisFactory : GenesisInterface {

    private val transactionCreatorId = "notary@notary"

    private val zeroPubKey = "0000000000000000000000000000000000000000000000000000000000000000"
    override fun getAccountsForConfiguration(peersCount: Int): List<AccountPrototype> {
        val activeAccounts =
            D3TestContext.d3neededAccounts.filter { it.type != AccountType.PASSIVE }
        activeAccounts
            .filter { it.peersDependentQuorum }
            .forEach { it.quorum = peersCount - peersCount / 3 }
        return activeAccounts
    }

    override fun getProject(): String = "D3"

    override fun getEnvironment(): String = "test"

    override fun createGenesisBlock(
        accounts: List<AccountPublicInfo>,
        peers: List<Peer>,
        blockVersion: String
    ): String {
        val transactionBuilder = Transaction.builder(null)
        transactionBuilder.setCreatorAccountId(transactionCreatorId)
        createPeers(peers, transactionBuilder)
        createRoles(transactionBuilder)
        createDomains(transactionBuilder)
        createAssets(transactionBuilder)
        createAccounts(transactionBuilder, accounts)
        createAccountDetails(transactionBuilder, peers)

        val blockBuilder = GenesisBlockBuilder().addTransaction(transactionBuilder.build().build())
        val block = blockBuilder.build()
        return JsonFormat.printer().omittingInsignificantWhitespace().print(block)
    }

    private fun createAccountDetails(
        transactionBuilder: TransactionBuilder,
        peers: List<Peer>
    ) {
        peers.forEach {
            transactionBuilder.setAccountDetail("notaries@notary", it.peerKey, it.notaryHostPort)
        }

    }

    private fun createAccounts(
        transactionBuilder: TransactionBuilder,
        accountsList: List<AccountPublicInfo>
    ) {
        val accountsMap: HashMap<String, AccountPublicInfo> = HashMap()
        accountsList.forEach { accountsMap.putIfAbsent(it.id, it) }
        checkAccountsGiven(accountsMap)

        D3TestContext.d3neededAccounts.forEach { account ->
            val accountPubInfo = accountsMap[account.id]
            if (accountPubInfo != null) {
                if (account.type == AccountType.IGNORED) {
                    if (accountPubInfo.pubKeys.size < accountPubInfo.quorum) {
                        throw AccountException("Needed account keys are not received: ${account.id}")
                    }
                } else {
                    if (accountPubInfo.pubKeys.isNotEmpty()) {
                        transactionBuilder.createAccount(
                            account.name,
                            account.domainId,
                            getIrohaPublicKeyFromHex(accountPubInfo.pubKeys[0])
                        )
                        if (accountPubInfo.pubKeys.size > 1) {
                            accountPubInfo.pubKeys
                                .filterIndexed { index, _ -> index > 0 }
                                .forEach { key ->
                                    transactionBuilder.addSignatory(
                                        account.id,
                                        getIrohaPublicKeyFromHex(key)
                                    )
                                }
                        }
                        if (account.peersDependentQuorum) {
                            transactionBuilder.setAccountQuorum(
                                accountPubInfo.id,
                                accountPubInfo.quorum
                            )
                        } else if (account.quorum <= accountPubInfo.quorum) {
                            transactionBuilder.setAccountQuorum(
                                accountPubInfo.id,
                                accountPubInfo.quorum
                            )
                        } else {
                            throw AccountException("Looks like account quorum in request is less than min quorum for this account: ${account.id}")
                        }
                    } else {
                        throw AccountException("Needed account keys are not received: ${account.id}")
                    }
                }
            } else if (account.type == AccountType.PASSIVE) {
                transactionBuilder.createAccount(
                    account.name,
                    account.domainId,
                    getIrohaPublicKeyFromHex(zeroPubKey)
                )
            } else {
                throw AccountException("Needed account keys are not received: ${account.id}")
            }
            account.roles.forEach {
                transactionBuilder.appendRole(account.id, it)
            }
        }
    }

    private fun checkAccountsGiven(accountsMap: HashMap<String, AccountPublicInfo>): List<String> {
        val errors = ArrayList<String>()
        D3TestContext.d3neededAccounts.forEach {
            if (!accountsMap.containsKey(it.id) && it.type != AccountType.PASSIVE) {
                errors.add("Needed account keys are not received: ${it.id}")
            } else if (it.type != AccountType.PASSIVE) {
                val pubKeysCount = accountsMap[it.id]?.pubKeys?.size ?: 0
                if (it.quorum > pubKeysCount || (accountsMap[it.id]?.quorum ?: 1 > pubKeysCount)) {
                    errors.add(
                        "Default or received quorum exceeds number of keys for account ${it.id}. " +
                                "Received(${accountsMap[it.id]?.quorum}) quorum should not be " +
                                "less than default(${it.quorum}) for this account"
                    )
                }
            }
        }
        if (errors.isNotEmpty()) {
            throw AccountException(errors.toString())
        }
        return errors
    }


    private fun createAssets(builder: TransactionBuilder) {
        // Sidechain-anchored
        createAsset(builder, "ether", "ethereum", 18)
        createAsset(builder, "btc", "bitcoin", 8)
    }

    private fun createDomains(builder: TransactionBuilder) {
        createDomain(builder, "notary", "none")
        createDomain(builder, "btcSession", "none")
        createDomain(builder, "ethereum", "none")
        createDomain(builder, "bitcoin", "none")
        createDomain(builder, "btcSignCollect", "none")
        createDomain(builder, "brvs", "brvs")
        createDomain(builder, "bootstrap", "none")
        createDomain(builder, "btcConsensus", "none")
    }

    private fun createRoles(builder: TransactionBuilder) {
        D3TestContext.createNotaryRole(builder)
        D3TestContext.createRelayDeployerRole(builder)
        D3TestContext.createEthTokenListStorageRole(builder)
        D3TestContext.createRegistrationServiceRole(builder)
        D3TestContext.createClientRole(builder)
        D3TestContext.createWithdrawalRole(builder)
        D3TestContext.createSignatureCollectorRole(builder)
        D3TestContext.createVacuumerRole(builder)
        D3TestContext.createNoneRole(builder)
        D3TestContext.createWhiteListSetterRole(builder)
        D3TestContext.createRollBackRole(builder)
        D3TestContext.createNotaryListHolderRole(builder)
        D3TestContext.createBrvsRole(builder)
        D3TestContext.createSuperuserRole(builder)
        D3TestContext.createAdminRole(builder)
        D3TestContext.createRmqRole(builder)
        D3TestContext.createBtcConsensusRole(builder)
        D3TestContext.createDataCollectorRole(builder)
        D3TestContext.createExchangeRole(builder)
        D3TestContext.createBillingRole(builder)
        D3TestContext.createBroadcastRole(builder)
    }
}
