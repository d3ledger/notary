package model

import java.security.KeyPair

/**
 * Class that represents account in Iroha
 * @param accountId - full id in Iroha: account name + domain name
 * @param keyPair - iroha Keypair associated with [accountId]
 */
data class IrohaCredential(
    val accountId: String,
    val keyPair: KeyPair
)