package com.d3.commons.registration

import com.d3.commons.notary.IrohaCommand
import com.d3.commons.notary.IrohaOrderedBatch
import com.d3.commons.notary.IrohaTransaction
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.consumer.IrohaConverter
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import iroha.protocol.Primitive
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.security.KeyPair

/**
 * Strategy to register client account in D3. This strategy creates only Iroha account.
 */
@Component
class NotaryRegistrationStrategy(
    @Autowired private val irohaConsumer: IrohaConsumer,
    @Autowired @Qualifier("clientStorageAccount") private val clientStorageAccount: String,
    @Autowired @Qualifier("brvsAccount") private val brvsAccount: String,
    @Autowired @Qualifier("primaryKeyPair") private val primaryKeyPair: KeyPair
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
        return createRegistrationBatch(name, domain, pubkey)
            .flatMap { batch ->
                irohaConsumer.send(batch).map { passedHashes ->
                    if (passedHashes.size != batch.size) {
                        throw IllegalStateException("Notary registration failed since tx batch was not fully successful")
                    }
                    "$name@$domain"
                }
            }
    }

    private fun createRegistrationBatch(
        name: String,
        domain: String,
        pubkey: String
    ): Result<List<TransactionOuterClass.Transaction>, Exception> {
        val newUserAccountId = "$name@$domain"

        val irohaBatch = IrohaOrderedBatch(
            listOf(
                IrohaTransaction(
                    irohaConsumer.creator,
                    ModelUtil.getCurrentTime(),
                    listOf(
                        IrohaCommand.CommandCreateAccount(
                            name,
                            domain,
                            Utils.toHex(primaryKeyPair.public.encoded)
                        ),
                        IrohaCommand.CommandSetAccountDetail(
                            clientStorageAccount,
                            "$name$domain",
                            domain
                        )
                    )
                ),

                IrohaTransaction(
                    newUserAccountId,
                    ModelUtil.getCurrentTime(),
                    listOf(
                        IrohaCommand.CommandGrantPermission(
                            brvsAccount,
                            Primitive.GrantablePermission.can_set_my_quorum_VALUE
                        ),
                        IrohaCommand.CommandGrantPermission(
                            brvsAccount,
                            Primitive.GrantablePermission.can_add_my_signatory_VALUE
                        ),
                        IrohaCommand.CommandGrantPermission(
                            brvsAccount,
                            Primitive.GrantablePermission.can_remove_my_signatory_VALUE
                        ),
                        IrohaCommand.CommandGrantPermission(
                            irohaConsumer.creator,
                            Primitive.GrantablePermission.can_add_my_signatory_VALUE
                        ),
                        IrohaCommand.CommandGrantPermission(
                            irohaConsumer.creator,
                            Primitive.GrantablePermission.can_remove_my_signatory_VALUE
                        )
                    )
                ),
                IrohaTransaction(
                    irohaConsumer.creator,
                    ModelUtil.getCurrentTime(),
                    listOf(
                        IrohaCommand.CommandAddSignatory(
                            newUserAccountId,
                            pubkey
                        ),
                        IrohaCommand.CommandSetAccountQuorum(
                            newUserAccountId,
                            2
                        )
                    )
                )
            )
        )

        val trueBatch = IrohaConverter.convertToUnsignedBatch(irohaBatch)

        return irohaConsumer.sign(
            trueBatch[0]
        ).fanout {
            irohaConsumer.sign(
                trueBatch[2]
            )
        }.map { (createAccountTx, newSignatoryTx) ->
            listOf<TransactionOuterClass.Transaction>(
                createAccountTx.build(),
                newSignatoryTx.build(),
                trueBatch[1]
                    .sign(primaryKeyPair)
                    .build()
            )
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
