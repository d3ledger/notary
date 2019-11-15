/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.provider

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.commons.util.GsonInstance
import com.d3.notifications.config.NotificationsConfig
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import mu.KLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.BigInteger

const val ETH_WITHDRAWAL_PROOF_DOMAIN = "ethWithdrawalProof"

/**
 * Provider that provider withdrawal proofs in the Ethereum subsystem
 */
@Component
class EthWithdrawalProofProvider(
    private val notificationsConfig: NotificationsConfig,
    @Qualifier("notaryQueryHelper")
    private val irohaQueryHelper: IrohaQueryHelper
) {

    private val gson = GsonInstance.get()

    /**
     * Return all withdrawal proofs
     * @param proofStorageAccount - account that is used to store proofs
     * @return set of proofs
     */
    fun getAllProofs(
        proofStorageAccount: String
    ): Result<Set<EthWithdrawalProof>, Exception> {
        return irohaQueryHelper.getAccountDetails(
            storageAccountId = proofStorageAccount,
            writerAccountId = notificationsConfig.notaryCredential.accountId
        ).map { details ->
            val proofs = HashSet<EthWithdrawalProof>()
            details.forEach { detail ->
                val proof = gson.fromJson(detail.value, EthWithdrawalProof::class.java)
                proofs.add(proof)
            }
            proofs
        }
    }

    /**
     * Checks if enough proofs were collected
     * @param proofs - proofs to check
     * @return true if enough, false otherwise
     */
    fun enoughProofs(proofs: Set<EthWithdrawalProof>): Result<Boolean, Exception> {
        if (proofs.isEmpty()) {
            return Result.of(false)
        }
        return irohaQueryHelper.getPeersCount().map { peers ->
            val superMajority = ((peers * 2) / 3) + 1
            //TODO check that proofs are valid
            proofs.size >= superMajority
        }
    }

    companion object : KLogging()
}

/**
 * Data class that represents withdrawal proof details
 */
data class EthWithdrawalProof(
    val tokenContractAddress: String,
    val amount: BigDecimal,
    val account: String,
    val irohaHash: String,
    val relay: String,
    val r: BigInteger,
    val s: BigInteger,
    val v: BigInteger
)
