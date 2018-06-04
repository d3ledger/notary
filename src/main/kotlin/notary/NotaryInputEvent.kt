package notary

import java.math.BigInteger

/**
 * All event [Notary] is waiting for.
 */
sealed class NotaryInputEvent {

    /**
     * Class represents events in Iroha chain
     */
    sealed class IrohaChainInputEvent : NotaryInputEvent() {

        /**
         * Event which raised on adding new peer in Iroha network
         */
        data class OnIrohaAddPeer(val address: String, val key: List<Byte>) : IrohaChainInputEvent()

        /**
         * Event which is raised when custodian transfer assets to notary account to withdraw asset
         *
         * @param asset is asset id in Iroha
         * @param amount of ethereum to withdraw
         */
        data class OnIrohaSideChainTransfer(
            val asset: String,
            val amount: BigInteger
        ) : IrohaChainInputEvent()
    }


    /**
     * Common class for all interested Ethereum events
     */
    sealed class EthChainInputEvent : NotaryInputEvent() {

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
        ) : EthChainInputEvent()

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
        ) : EthChainInputEvent()
    }

}
