package com.d3.commons.registration

import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Strategy to register client account in D3. This strategy creates only Iroha account.
 */
@Component
class NotaryRegistrationStrategy(
    @Autowired private val irohaConsumer: IrohaConsumer
) : RegistrationStrategy {

    /**
     * Register a new D3 client in Iroha
     * @param accountName - unique user name
     * @param domainId - client domain
     * @param publicKey - client public key
     * @return hash of tx in Iroha
     */
    override fun register(
        accountName: String,
        domainId: String,
        whitelist: List<String>,
        publicKey: String
    ): Result<String, Exception> {
        logger.info { "notary registration of client $accountName@$domainId with pubkey $publicKey" }
        return ModelUtil.createAccount(
            irohaConsumer,
            accountName,
            domainId,
            Utils.parseHexPublicKey(publicKey)
        ).map {
            "$accountName@$domainId"
        }
    }

    override fun getFreeAddressNumber(): Result<Int, Exception> {
        return Result.of { throw Exception("not supported") }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
