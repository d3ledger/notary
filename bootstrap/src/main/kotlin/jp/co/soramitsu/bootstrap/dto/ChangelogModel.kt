package jp.co.soramitsu.bootstrap.dto

data class ChangelogRequestDetails(
    val accounts: List<AccountPublicInfo> = emptyList(),
    val peers: List<Peer> = emptyList(),
    val meta: ProjectEnv = ProjectEnv(),
    val irohaConfig: IrohaConfig = IrohaConfig(),
    val superuserKeys: List<ClientKeyPair> = emptyList()
)

data class ChangelogFileRequest(
    val changelogFile: String = "",
    val details: ChangelogRequestDetails = ChangelogRequestDetails()
)

data class ChangelogScriptRequest(
    val script: String = "",
    val details: ChangelogRequestDetails = ChangelogRequestDetails()
)

data class ClientKeyPair(val publicKey: String = "", val privateKey: String = "")