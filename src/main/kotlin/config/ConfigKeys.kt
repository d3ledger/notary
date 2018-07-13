package config

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
        val notaryIrohaHostname = Key("notary.iroha.hostname", stringType)
        /** Iroha peer port */
        val notaryIrohaPort = Key("notary.iroha.port", intType)
        val notaryIrohaAccount = Key("notary.iroha.account", stringType)
        /** Path to public key of Iroha transactions creator */
        val notaryPubkeyPath = Key("notary.iroha.pubkey-path", stringType)
        /** Path to private key of Iroha transactions creator */
        val notaryPrivkeyPath = Key("notary.iroha.privkey-path", stringType)

        // --------- Ethereum ---------
        /** URL of Ethereum client */
        val notaryEthConnectionUrl = Key("notary.ethereum.url", stringType)

        // --------- Refund notary.endpoint ---------
        /** Port for refund REST API */
        val notaryRefundPort = Key("notary.refund.port", intType)
        /** URL of refund for ETH side chain */
        val notaryEthEndpoint = Key("notary.refund.endpoint.ethereum", stringType)


        // ========= Relay Registration =========

        // --------- Iroha ---------
        /** Iroha peer hostname */
        val relayRegistrationIrohaHostname = Key("relay-registration.iroha.hostname", stringType)
        /** Iroha peer port */
        val relayRegistrationIrohaPort = Key("relay-registration.iroha.port", intType)
        /** Notary account in Iroha */
        val relayRegistrationIrohaAccount = Key("relay-registration.iroha.account", stringType)
        /** Path to public key of Iroha transactions creator */
        val relayRegistrationPubkeyPath = Key("relay-registration.iroha.pubkey-path", stringType)
        /** Path to private key of Iroha transactions creator */
        val relayRegistrationPrivkeyPath = Key("relay-registration.iroha.privkey-path", stringType)
        /** Account to store registered free wallets */
        val relayRegistrationNotaryIrohaAccount = Key("relay-registration.iroha.notary-account", stringType)

        // --------- Ethereum ---------
        /** URL of Ethereum client */
        val relayRegistartionEthConnectionUrl = Key("relay-registration.ethereum.url", stringType)
        /** Path to Ethereum credentials */
        val relayRegistartionEthCredentialPath = Key("relay-registration.ethereum.credentials-path", stringType)
        /** Password for Ethereum credentials */
        val relayRegistartionEthCredentialPassword = Key("relay-registration.ethereum.credentials-password", stringType)
        /** Gas price for relay smart contract deployment */
        val relayRegistartionEthGasPrice = Key("relay-registration.ethereum.gas-price", longType)
        /** Gas limit for relay smart contract deployment */
        val relayRegistartionEthGasLimit = Key("relay-registration.ethereum.gas-limit", longType)


        // ========= Registration Service =========
        val registrationPort = Key("registration.port", intType)
        /** Iroha peer hostname */
        val registrationServiceIrohaHostname = Key("registration.iroha.hostname", stringType)
        /** Iroha peer port */
        val registrationServiceIrohaPort = Key("registration.iroha.port", intType)
        /** Iroha peer hostname */
        val registrationServiceIrohaAccount = Key("registration.iroha.account", stringType)
        /** Account to store registered free wallets */
        val registrationServiceNotaryIrohaAccount = Key("registration.iroha.notary-account", stringType)
        /** Account of relay registration service */
        val registrationServiceRelayRegistrationIrohaAccount =
            Key("registrationService.iroha.relay-registration-account", stringType)
        /** Path to public key of Iroha transactions creator */
        val registrationServicePubkeyPath = Key("registration.iroha.pubkey-path", stringType)
        /** Path to private key of Iroha transactions creator */
        val registrationServicePrivkeyPath = Key("registration.iroha.privkey-path", stringType)


        // ========= Test =========
        /** Iroha peer hostname */
        val testIrohaHostname = Key("test.iroha.hostname", stringType)
        /** Iroha peer port */
        val testIrohaPort = Key("test.iroha.port", intType)
        /** Notary account in Iroha */
        val testIrohaAccount = Key("test.iroha.account", stringType)
        /** Path to public key of Iroha transactions creator */
        val testPubkeyPath = Key("test.iroha.pubkey-path", stringType)
        /** Path to private key of Iroha transactions creator */
        val testPrivkeyPath = Key("test.iroha.privkey-path", stringType)
        /** URL of Ethereum client */
        val testEthConnectionUrl = Key("test.ethereum.url", stringType)

        /** Ethereum token name in Iroha */
        val irohaEthToken = Key("iroha.ether-token", stringType)

        // --------- Ethereum ---------
        /** Confirmation period */
        val ethConfirmationPeriod = Key("ethereum.confirmation-period", longType)

        // --------- Database ----------
        /** url of notary db instance */
        val dbUrl = Key("db.url", stringType)
        /** username to connect to notary db instance */
        val dbUsername = Key("db.username", stringType)
        /** password to connect to notary db instance */
        val dbPassword = Key("db.password", stringType)
    }
}
