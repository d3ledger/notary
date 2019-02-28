package com.d3.commons.registration

import com.github.kittinunf.result.Result
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.util.ModelUtil

/**
 * Strategy to register client account in D3. This strategy creates only Iroha account.
 */
@Component
class NotaryRegistrationStrategy(
    @Autowired private val irohaConsumer: IrohaConsumer
) : RegistrationStrategy {

    /**
     * Register a new D3 client in Iroha
     * @param name - unique user name
     * @param domain - client domain
     * @param pubkey - client public key
     * @return hash of tx in Iroha
     */
    override fun register(
        name: String,
        domain: String,
        whitelist: List<String>,
        pubkey: String
    ): Result<String, Exception> {
        logger.info { "notary registration of client $name with pubkey $pubkey" }
        return ModelUtil.createAccount(
            irohaConsumer,
            name,
            domain,
            Utils.parseHexPublicKey(pubkey)
        )
    }

    override fun getFreeAddressNumber(): Result<Int, Exception> {
        return Result.of { throw Exception("not supported") }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
