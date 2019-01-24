package model

import jp.co.soramitsu.iroha.Keypair


/**
 * Class that represents account in Iroha
 * @param accountId - full id in Iroha: account name + domain name
 * @param keyPair - iroha Keypair associated with [accountId]
 */
data class IrohaCredential(
    val accountId: String,
    val keyPair: Keypair
)