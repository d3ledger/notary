package sideChain

/**
 * Class represents events in Iroha chain
 */
sealed class IrohaChainEvent

/**
 * Event which raised on adding new peer in Iroha network
 */
abstract class OnIrohaAddPeer : IrohaChainEvent()

/**
 * Event which is raised when custodian transfer assets to notary account
 */
abstract class onIrohaSideChainTransfer : IrohaChainEvent()
