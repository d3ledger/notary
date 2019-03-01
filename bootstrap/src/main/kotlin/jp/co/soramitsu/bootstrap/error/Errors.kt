package jp.co.soramitsu.bootstrap.error

import java.lang.RuntimeException

class AccountException(message:String) : RuntimeException(message)
class IrohaPublicKeyError(message:String) : RuntimeException(message)