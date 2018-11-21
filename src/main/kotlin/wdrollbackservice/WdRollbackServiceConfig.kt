package wdrollbackservice

import config.IrohaConfig
import config.IrohaCredentialConfig

/** Configuration of withdrawal rollback service */
interface WdRollbackServiceConfig {

    /** Notary storage account in Iroha */
    val notaryListStorageAccount: String

    /** Account who sets account details */
    val notaryListSetterAccount: String

    /** Account who stores and sets tx storage details */
    val withdrawalTxStorageAccount: String

    val withdrawalCredential: IrohaCredentialConfig

    /** Iroha configuration */
    val iroha: IrohaConfig
}
