package com.d3.commons.provider

typealias PeerAddress = String

/**
 * Provides with list of all notaries
 */
interface NotaryPeerListProvider {
    fun getPeerList(): List<PeerAddress>
}
