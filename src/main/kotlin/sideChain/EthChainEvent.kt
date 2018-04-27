package sideChain


/**
 * Common class for all interested Ethereum events
 */
sealed class EthChainEvent {

    /**
     * Event which raised on deploying target smart contract in the Ethereum
     */
    class OnDeployContract

    /**
     * Event which raised on new transfer transaction to Iroha
     */
    class OnTransfer

    /**
     * Event which raised on new transaction with new peer in the contract
     */
    class OnAddPeer
}
