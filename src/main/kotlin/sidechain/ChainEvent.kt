package sidechain

import java.math.BigInteger
import java.util.*

/**
 * All events emitted by side chains.
 */
sealed class SideChainEvent {

    /**
     * Class represents events in Iroha chain
     */
    sealed class IrohaEvent : SideChainEvent() {

        /**
         * Event which raised on adding new peer in Iroha network
         *
         * @param address peer's address, ip and port
         * @param key peer's key
         */
        data class OnIrohaAddPeer(
            val address: String,
            val key: ByteArray
        ) : IrohaEvent() {

            /** equals is required by array field */
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other?.javaClass != javaClass) return false

                other as OnIrohaAddPeer
                return (address == other.address) && (Arrays.equals(key, other.key))
            }

            /** hashCode is required by array field */
            override fun hashCode(): Int {
                return Objects.hash(address, Arrays.hashCode(key))
            }

        }

        /**
         * Event which is raised when custodian transfer assets to notary account to withdraw asset
         *
         * @param asset is asset id in Iroha
         * @param amount of ethereum to withdraw
         */
        data class OnIrohaSideChainTransfer(
            val asset: String,
            val amount: BigInteger
        ) : IrohaEvent()
    }

    /**
     * Common class for all interested Ethereum events
     */
    sealed class EthereumEvent : SideChainEvent() {

        /**
         * Event which raised on a new transfer transaction to Ethereum wallet
         * @param hash transaction hash
         * @param user user name in Iroha
         * @param amount amount of Ether transfered
         */
        data class OnEthSidechainDeposit(
            val hash: String,
            val user: String,
            val amount: BigInteger
        ) : EthereumEvent()

        /**
         * Event which occures when custodian deposits ERC20 token
         * @param hash transaction hash
         * @param user user name in Iroha
         * @param token token name
         * @param amount amount of tokens
         */
        data class OnEthSidechainDepositToken(
            val hash: String,
            val user: String,
            val token: String,
            val amount: BigInteger
        ) : EthereumEvent()
    }
}
