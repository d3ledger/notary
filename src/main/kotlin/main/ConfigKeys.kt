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
        // ========= Notary =========
        // --------- Iroha ---------
        /** Notary account in Iroha */
        /** Iroha peer hostname */
        val notaryIrohaHostname = Key("notary.irohaHostname", stringType)
        /** Iroha peer port */
        val notaryIrohaPort = Key("notary.irohaPort", intType)
        val notaryIrohaAccount = Key("notary.irohaAccount", stringType)
        /** Path to public key of Iroha transactions creator */
        val notaryPubkeyPath = Key("notary.pubkeyPath", stringType)
        /** Path to private key of Iroha transactions creator */
        val notaryPrivkeyPath = Key("notary.privkeyPath", stringType)

        // --------- Ethereum ---------
        /** URL of Ethereum client */
        val notaryEthConnectionUrl = Key("notary.ethConnectionUrl", stringType)

        // --------- Refund endpoint ---------
        /** Port for refund REST API */
        val notaryRefundPort = Key("notary.refundPort", intType)
        /** URL of refund for ETH side chain */
        val notaryEthEndpoint = Key("notary.ethEndpoint", stringType)


        // ========= Relay Registration =========

        // --------- Iroha ---------
        /** Iroha peer hostname */
        val relayRegistrationIrohaHostname = Key("relayRegistration.irohaHostname", stringType)
        /** Iroha peer port */
        val relayRegistrationIrohaPort = Key("relayRegistration.irohaPort", intType)
        /** Notary account in Iroha */
        val relayRegistrationIrohaAccount = Key("relayRegistration.irohaAccount", stringType)
        /** Path to public key of Iroha transactions creator */
        val relayRegistrationPubkeyPath = Key("relayRegistration.pubkeyPath", stringType)
        /** Path to private key of Iroha transactions creator */
        val relayRegistrationPrivkeyPath = Key("relayRegistration.privkeyPath", stringType)
        /** Account to store registered free wallets */
        val relayRegistrationNotaryIrohaAccount = Key("relayRegistration.notaryIrohaAccount", stringType)

        // --------- Ethereum ---------
        /** URL of Ethereum client */
        val relayRegistartionEthConnectionUrl = Key("relayRegistration.ethConnectionUrl", stringType)
        /** Path to Ethereum credentials */
        val relayRegistartionEthCredentialPath = Key("relayRegistration.ethCredentialPath", stringType)
        /** Password for Ethereum credentials */
        val relayRegistartionEthCredentialPassword = Key("relayRegistration.ethCredentialPassword", stringType)
        /** Gas price for relay smart contract deployment */
        val relayRegistartionEthGasPrice = Key("relayRegistration.ethGasPrice", longType)
        /** Gas limit for relay smart contract deployment */
        val relayRegistartionEthGasLimit = Key("relayRegistration.ethGasLimit", longType)


        // ========= Registration Service =========
        val registrationPort = Key("registrationPort", intType)
        /** Iroha peer hostname */
        val registrationServiceIrohaHostname = Key("registrationService.irohaHostname", stringType)
        /** Iroha peer port */
        val registrationServiceIrohaPort = Key("registrationService.irohaPort", intType)
        /** Iroha peer hostname */
        val registrationServiceIrohaAccount = Key("registrationService.irohaAccount", stringType)
        /** Account to store registered free wallets */
        val registrationServiceNotaryIrohaAccount = Key("registrationService.notaryIrohaAccount", stringType)
        /** Account of relay registration service */
        val registrationServiceRelayRegistrationIrohaAccount =
            Key("registrationService.relayRegistrationIrohaAccount", stringType)
        /** Path to public key of Iroha transactions creator */
        val registrationServicePubkeyPath = Key("registrationService.pubkeyPath", stringType)
        /** Path to private key of Iroha transactions creator */
        val registrationServicePrivkeyPath = Key("registrationService.privkeyPath", stringType)


        // ========= Test =========
        /** Iroha peer hostname */
        val testIrohaHostname = Key("test.irohaHostname", stringType)
        /** Iroha peer port */
        val testIrohaPort = Key("test.irohaPort", intType)
        /** Notary account in Iroha */
        val testIrohaAccount = Key("test.irohaAccount", stringType)
        /** Path to public key of Iroha transactions creator */
        val testPubkeyPath = Key("test.pubkeyPath", stringType)
        /** Path to private key of Iroha transactions creator */
        val testPrivkeyPath = Key("test.privkeyPath", stringType)
        /** URL of Ethereum client */
        val testEthConnectionUrl = Key("test.ethConnectionUrl", stringType)

        /** Ethereum token name in Iroha */
        val irohaEthToken = Key("irohaEtherToken", stringType)

        // --------- Ethereum ---------
        /** Confirmation period */
        val ethConfirmationPeriod = Key("ethConfirmationPeriod", longType)

        // --------- Database ----------
        /** url of notary db instance */
        val dbUrl = Key("dbUrl", stringType)
        /** username to connect to notary db instance */
        val dbUsername = Key("dbUsername", stringType)
        /** password to connect to notary db instance */
        val dbPassword = Key("dbPassword", stringType)
    }
}
