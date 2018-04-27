package sideChain


/**
 * Common class for all interested Ethereum events
 */
sealed class EthChainEvent

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
