package jp.co.soramitsu.bootstrap.genesis

import jp.co.soramitsu.bootstrap.dto.AccountPrototype
import jp.co.soramitsu.bootstrap.dto.AccountPublicInfo
import jp.co.soramitsu.bootstrap.dto.IrohaAccountDto
import jp.co.soramitsu.bootstrap.dto.Peer

interface GenesisInterface {

    fun getProject(): String
    fun getEnvironment(): String
    fun createGenesisBlock(accounts:List<AccountPublicInfo>, peers:List<Peer>, blockVersion:String = "1"): String
    fun getAccountsForConfiguration(peersCount:Int): List<AccountPrototype>
}
