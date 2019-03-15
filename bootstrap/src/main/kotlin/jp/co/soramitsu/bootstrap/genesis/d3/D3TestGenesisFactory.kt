package jp.co.soramitsu.bootstrap.genesis.d3

import com.google.protobuf.util.JsonFormat
import jp.co.soramitsu.bootstrap.dto.AccountPrototype
import jp.co.soramitsu.bootstrap.dto.AccountPublicInfo
import jp.co.soramitsu.bootstrap.dto.Peer
import jp.co.soramitsu.bootstrap.exceptions.AccountException
import jp.co.soramitsu.bootstrap.genesis.*
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.TransactionBuilder
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import java.util.*

class D3TestGenesisFactory : GenesisInterface {

    private val zeroPubKey = "0000000000000000000000000000000000000000000000000000000000000000"
    override fun getAccountsNeeded(): List<AccountPrototype> = D3TestContext.d3neededAccounts

    override fun getProject(): String = "D3"

    override fun getEnvironment(): String = "test"

    override fun createGenesisBlock(
        accounts: List<AccountPublicInfo>,
        peers: List<Peer>,
        blockVersion: String
    ): String {
        val transactionBuilder = Transaction.builder(null)

        createPeers(peers, transactionBuilder)
        createRoles(transactionBuilder)
        createDomains(transactionBuilder)
        createAssets(transactionBuilder)
        createAccounts(transactionBuilder, accounts, peers.size)

        val blockBuilder = GenesisBlockBuilder().addTransaction(transactionBuilder.build().build())
        val block = blockBuilder.build()
        val payload = JsonFormat.printer().omittingInsignificantWhitespace().print(block)
        return "{\"blockV1\": $payload}"
    }

    private fun createAccounts(
        transactionBuilder: TransactionBuilder,
        accountsList: List<AccountPublicInfo>,
        peersCount: Int
    ) {
        val accountsMap: HashMap<String, AccountPublicInfo> = HashMap()
        accountsList.forEach { accountsMap.putIfAbsent("${it.accountName}@${it.domainId}", it) }

        val accountErrors = checkAccountsGiven(accountsMap)
        if (accountErrors.isNotEmpty()) {
            throw AccountException(accountErrors.toString())
        }
        D3TestContext.d3neededAccounts.forEach { account ->
            val accountPubInfo = accountsMap[account.id]
            if (accountPubInfo != null) {
                if (accountPubInfo.pubKeys.isNotEmpty()) {
                    transactionBuilder.createAccount(
                        account.title,
                        account.domainId,
                        getIrohaPublicKeyFromHex(accountPubInfo.pubKeys[0])
                    )
                    accountPubInfo.pubKeys.subList(1, accountPubInfo.pubKeys.size)
                        .forEach { key ->
                            transactionBuilder.addSignatory(
                                account.id,
                                getIrohaPublicKeyFromHex(key)
                            )
                        }
                } else {
                    throw AccountException("Needed account keys are not received: ${account.id}")
                }
            } else if (account.passive) {
                transactionBuilder.createAccount(
                    account.title,
                    account.domainId,
                    getIrohaPublicKeyFromHex(zeroPubKey)
                )
            } else {
                throw AccountException("Needed account keys are not received: ${account.id}")
            }
        }
    }

    private fun checkAccountsGiven(accountsMap: HashMap<String, AccountPublicInfo>): List<String> {
        val loosed = ArrayList<String>()
        D3TestContext.d3neededAccounts.forEach {
            if (!accountsMap.containsKey(it.id) && !it.passive) {
                loosed.add("Needed account keys are not received: ${it.id}")
            } else if (!it.passive) {
                val pubKeysCount = accountsMap[it.id]?.pubKeys?.size ?: 0
                if (it.quorum > pubKeysCount) {
                    loosed.add("Quorum exceeds number of keys(${it.quorum}>$pubKeysCount) for account ${it.id}")
                }
            }
        }
        return loosed
    }


    private fun createAssets(builder: TransactionBuilder) {
        createAsset(builder, "xor", "sora", 18)
        createAsset(builder, "ether", "ethereum", 18)
        createAsset(builder, "btc", "bitcoin", 8)
    }

    private fun createDomains(builder: TransactionBuilder) {
        createDomain(builder, "notary", "none")
        createDomain(builder, "d3", "client")
        createDomain(builder, "btcSession", "none")
        createDomain(builder, "ethereum", "none")
        createDomain(builder, "sora", "sora_client")
        createDomain(builder, "bitcoin", "client")
        createDomain(builder, "btcSignCollect", "none")
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
        D3TestContext.createTesterRole(builder)
        D3TestContext.createWhiteListSetterRole(builder)
        D3TestContext.createRollBackRole(builder)
        D3TestContext.createNotaryListHolderRole(builder)
        D3TestContext.createSoraClientRole(builder)
        D3TestContext.createBtcFeeRateSetterRole(builder)
    }

}


