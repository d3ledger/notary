package jp.co.soramitsu.bootstrap.exceptions

class AccountException(message: String) : RuntimeException(message)
class IrohaPublicKeyException(message: String) : RuntimeException(message)

enum class ErrorCodes {
    INCORRECT_PEERS_COUNT,
    EMPTY_PEER_PUBLIC_KEY,
    NO_GENESIS_FACTORY,
    EMPTY_ACCOUNT_PUBLIC_KEY
}
