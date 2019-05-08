/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

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
     * @param accountName - unique user name
     * @param domainId - client domain
     * @param publicKey - client public key
     * @return hash of tx in Iroha
     */
    override fun register(
        accountName: String,
        domainId: String,
        publicKey: String
    ): Result<String, Exception> {
        logger.info { "notary registration of client $accountName with pubkey $publicKey" }
        return createRegistrationBatch(accountName, domainId, publicKey)
            .flatMap { batch ->
                irohaConsumer.send(batch).map { passedHashes ->
                    if (passedHashes.size != batch.size) {
                        throw IllegalStateException("Notary registration failed since tx batch was not fully successful")
                    }
                    "$accountName@$domainId"
                }
            }
    }

    /**
     * Creates the registration batch allowing BRVS receive needed power relatively to the user
     */
    private fun createRegistrationBatch(
        name: String,
        domain: String,
        pubkey: String
    ): Result<List<TransactionOuterClass.Transaction>, Exception> {
        val newUserAccountId = "$name@$domain"

        val irohaBatch = IrohaOrderedBatch(
            listOf(
                // First step is to create user account but with our own key, not user's one
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
                // Second step is to give permissions from the user to brvs and registration service
                // Here we need our own key to sign this stuff
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
                            Primitive.GrantablePermission.can_set_my_quorum_VALUE
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
                // Finally we need to add user's original pub key to the signatories list
                // But we need to increase user's quorum to prohibit transactions using only user's key
                // Several moments later BRVS react on the createAccountTransaction and
                // user's quorum will be set as 1+2/3 of BRVS instances for now
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
                trueBatch[1]
                    .sign(primaryKeyPair)
                    .build(),
                newSignatoryTx.build()
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
