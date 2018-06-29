package main

import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.longType
import com.natpryce.konfig.stringType

/**
 * Configuration parameters description.
 */
class ConfigKeys {
    companion object {
        /** Port for refund REST API */
        val refundPort = Key("refundPort", intType)

        // --------- Iroha ---------
        /** Path to public key of Iroha transactions creator */
        val pubkeyPath = Key("pubkeyPath", stringType)
        /** Path to private key of Iroha transactions creator */
        val privkeyPath = Key("privkeyPath", stringType)

        /** Iroha peer hostname */
        val irohaHostname = Key("irohaHostname", stringType)
        /** Iroha peer port */
        val irohaPort = Key("irohaPort", intType)

        /** Iroha account of transactions creator */
        val irohaCreator = Key("irohaCreator", stringType)
        /** Ethereum token name in Iroha */
        val irohaEthToken = Key("irohaEtherToken", stringType)
        /** Master account in Iroha */
        val irohaMaster = Key("irohaMaster", stringType)


        // --------- Ethereum ---------
        /** Confirmation period */
        val ethConfirmationPeriod = Key("ethConfirmationPeriod", longType)
        /** URL of Ethereum client */
        val ethConnectionUrl = Key("ethConnectionUrl", stringType)
        /** URL of refund for ETH side chain */
        val ethEndpoint = Key("ethEndpoint", stringType)
        /** Path to Ethereum credentials */
        val ethCredentialPath = Key("ethCredentialPath", stringType)
        /** Password for Ethereum credentials */
        val ethCredentialPassword = Key("ethCredentialPassword", stringType)
        /** Gas price for relay smart contract deployment */
        val ethGasPrice = Key("ethGasPrice", longType)
        /** Gas limit for relay smart contract deployment */
        val ethGasLimit = Key("ethGasLimit", longType)


        // --------- Database ----------
        /** url of notary db instance */
        val dbUrl = Key("dbUrl", stringType)
        /** username to connect to notary db instance */
        val dbUsername = Key("dbUsername", stringType)
        /** password to connect to notary db instance */
        val dbPassword = Key("dbPassword", stringType)

        // --------- Registration Service ---------
        val registrationPort = Key("registrationPort", intType)
    }
}
