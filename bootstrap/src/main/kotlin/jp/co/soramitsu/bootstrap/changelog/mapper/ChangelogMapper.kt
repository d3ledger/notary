package jp.co.soramitsu.bootstrap.changelog.mapper

import jp.co.soramitsu.bootstrap.changelog.ChangelogAccountPublicInfo
import jp.co.soramitsu.bootstrap.changelog.ChangelogPeer
import jp.co.soramitsu.bootstrap.dto.AccountPublicInfo
import jp.co.soramitsu.bootstrap.dto.Peer

/**
 * Maps AccountPublicInfo to ChangelogAccountPublicInfo
 * @param accountPublicInfo - account info to map
 * @return ChangelogAccountPublicInfo
 */
fun toChangelogAccount(accountPublicInfo: AccountPublicInfo): ChangelogAccountPublicInfo {
    val changelogAccount = ChangelogAccountPublicInfo()
    changelogAccount.accountName = accountPublicInfo.accountName
    changelogAccount.domainId = accountPublicInfo.domainId
    changelogAccount.pubKeys = accountPublicInfo.pubKeys
    changelogAccount.quorum = accountPublicInfo.quorum
    return changelogAccount
}

/**
 * Maps Peer to ChangelogPeer
 * @param peer - peer to map
 * @return ChangelogPeer
 */
fun toChangelogPeer(peer: Peer): ChangelogPeer {
    val changelogPeer = ChangelogPeer()
    changelogPeer.hostPort = peer.hostPort
    changelogPeer.peerKey = peer.peerKey
    return changelogPeer
}
