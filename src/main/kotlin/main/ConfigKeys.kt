package main

import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.longType
import com.natpryce.konfig.stringType

class ConfigKeys {
    companion object {
        val refundPort = Key("refundPort", intType)

        val pubkeyPath = Key("pubkeyPath", stringType)
        val privkeyPath = Key("privkeyPath", stringType)

        val irohaCreator = Key("irohaCreator", stringType)
        val irohaHostname = Key("irohaHostname", stringType)
        val irohaPort = Key("irohaPort", intType)

        val ethConfirmationPeriod = Key("ethConfirmationPeriod", longType)
        val ethListenAddress = Key("ethListenAddress", stringType)
        val ethConnectionUrl = Key("ethConnectionUrl", stringType)
    }

}