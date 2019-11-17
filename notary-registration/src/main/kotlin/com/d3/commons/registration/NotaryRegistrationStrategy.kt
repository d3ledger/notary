/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.registration

import com.d3.commons.model.D3ErrorException
import com.d3.commons.notary.IrohaCommand
import com.d3.commons.notary.IrohaOrderedBatch
import com.d3.commons.notary.IrohaTransaction
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.consumer.IrohaConverter
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import iroha.protocol.Primitive
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.ErrorResponseException
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.security.KeyPair

/**
 * Strategy to register client account in D3. This strategy creates only Iroha account.
 */
@Component
class NotaryRegistrationStrategy(
    private val irohaConsumer: IrohaConsumer,
    private val queryHelper: IrohaQueryHelper,
    @Qualifier("clientStorageAccount") private val clientStorageAccount: String,
    @Qualifier("brvsAccount") private val brvsAccount: String,
    @Qualifier("primaryKeyPair") private val primaryKeyPair: KeyPair,
    private val isBrvsEnabled: Boolean
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
        logger.info("notary registration of client $accountName@$domainId with pubkey $publicKey")
        return queryHelper.isRegistered(accountName, domainId, publicKey).flatMap { registered ->
            if (registered) {
                logger.info { "client $accountName@$domainId is already registered" }
                createSuccessResult(accountName, domainId)
            } else {
                createRegistrationBatch(accountName, domainId, publicKey).flatMap { batch ->
                    sendBatch(accountName, domainId, batch)
                }.flatMap {
                    createSuccessResult(accountName, domainId)
                }
            }
        }
    }

    /**
     * Returns successful response
     */
    fun createSuccessResult(accountName: String, domainId: String) =
        Result.of { "$accountName@$domainId" }

    /**
     * Send list of transactions as batch
     */
    fun sendBatch(
        accountName: String,
        domainId: String,
        batch: List<TransactionOuterClass.Transaction>
    ) = irohaConsumer.send(batch).map { passedHashes ->
        if (passedHashes.size != batch.size) {
            throw D3ErrorException.warning(
                failedOperation = NOTARY_REGISTRATION_OPERATION,
                description = "Notary registration of account $accountName@$domainId failed since tx batch was not fully successful"
            )
        }
    }

    /**
     * Creates the registration batch allowing BRVS receive needed power relatively to the user if needed
     */
    private fun createRegistrationBatch(
        name: String,
        domain: String,
        pubkey: String
    ): Result<List<TransactionOuterClass.Transaction>, Exception> {
        val newUserAccountId = "$name@$domain"

        val transactions = ArrayList<IrohaTransaction>()

        transactions.add(
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
            )
        )
        if (isBrvsEnabled) {
            transactions.add(
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
                )
            )
        }
        transactions.add(
            // Finally we need to add user's original pub key to the signatories list
            // But we need to increase user's quorum to prohibit transactions using only user's key
            // Several moments later BRVS react on the createAccountTransaction and
            // user's quorum will be set as 1+2/3 of BRVS instances for now
            getSignatoryTx(newUserAccountId, pubkey)
        )

        val irohaBatch = IrohaOrderedBatch(transactions)

        return Result.of {
            IrohaConverter.convertToUnsignedBatch(irohaBatch)
        }.flatMap { trueBatch ->
            irohaConsumer.sign(
                trueBatch[0]
            )
                .map { createAccountTx ->
                    val irohaTransactions = ArrayList<TransactionOuterClass.Transaction>()
                    irohaTransactions.add(createAccountTx.build())
                    if (isBrvsEnabled) {
                        irohaTransactions.add(
                            trueBatch[1]
                                .sign(primaryKeyPair)
                                .build()
                        )
                    }
                    irohaTransactions.add(
                        trueBatch[if (isBrvsEnabled) 2 else 1]
                            .sign(primaryKeyPair)
                            .build()
                    )
                    irohaTransactions
                }
        }
    }

    override fun getFreeAddressNumber(): Result<Int, Exception> {
        return Result.of { throw UnsupportedOperationException() }
    }

    private fun getSignatoryTx(accountId: String, publicKey: String): IrohaTransaction {
        val commands = ArrayList<IrohaCommand>()
        commands.add(
            IrohaCommand.CommandAddSignatory(
                accountId,
                publicKey
            )
        )
        if (isBrvsEnabled) {
            commands.add(
                IrohaCommand.CommandSetAccountQuorum(
                    accountId,
                    DEFAULT_BRVS_QUORUM
                )
            )
        }
        return IrohaTransaction(
            accountId,
            ModelUtil.getCurrentTime(),
            commands
        )
    }

    /**
     * Logger
     */
    companion object : KLogging() {
        const val DEFAULT_BRVS_QUORUM = 2
    }
}
