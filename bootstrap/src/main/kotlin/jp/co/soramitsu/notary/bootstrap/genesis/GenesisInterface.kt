package jp.co.soramitsu.notary.bootstrap.genesis

import jp.co.soramitsu.notary.bootstrap.dto.AccountPrototype
import jp.co.soramitsu.notary.bootstrap.dto.IrohaAccountDto
import jp.co.soramitsu.notary.bootstrap.dto.Peer

interface GenesisInterface {

    fun getProject(): String
    fun getEnvironment(): String
    fun createGenesisBlock(accounts:List<IrohaAccountDto>, peers:List<Peer>, blockVersion:String = "1"): String
    fun getAccountsNeeded(): List<AccountPrototype>
}