package algorithm

/**
 * All event [Notary] is waiting for.
 */
sealed class NotaryEvent {

    /**
     * Class represents events in Iroha chain
     */
    sealed class IrohaChainEvent : NotaryEvent() {

        /**
         * Event which raised on adding new peer in Iroha network
         */
        abstract class OnIrohaAddPeer : IrohaChainEvent()

        /**
         * Event which is raised when custodian transfer assets to notary account
         */
        abstract class OnIrohaSideChainTransfer : IrohaChainEvent()
    }


    /**
     * Common class for all interested Ethereum events
     */
    sealed class EthChainEvent : NotaryEvent() {

        /**
         * Event which raised on deploying target smart contract in the Ethereum
         */
        abstract class OnEthDeployContract : EthChainEvent()

        /**
         * Event which raised on new transfer transaction to Iroha
         */
        abstract class OnEthSidechainTransfer : EthChainEvent()

        /**
         * Event which raised on new transaction with new peer in the contract
         */
        abstract class OnEthAddPeer : EthChainEvent()
    }

}