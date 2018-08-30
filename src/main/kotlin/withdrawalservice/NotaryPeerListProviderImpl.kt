package withdrawalservice

/**
 * Provides with list of all notaries peers in the system
 */
class NotaryPeerListProviderImpl() : NotaryPeerListProvider {

    override fun getPeerList(): List<PeerAddress> {
        // TODO replace with effective implementation
        // load from DB, think about hot addition of new peers
        return listOf("http://localhost:10001")
    }
}
