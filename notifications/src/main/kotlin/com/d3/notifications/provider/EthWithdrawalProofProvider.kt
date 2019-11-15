/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.provider

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.commons.util.GsonInstance
import com.d3.notifications.config.ETHSpecificConfig
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import mu.KLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Provider that provider withdrawal proofs in the Ethereum subsystem
 */
@Component
class EthWithdrawalProofProvider(
    private val ethSpecificConfig: ETHSpecificConfig,
    @Qualifier("notaryQueryHelper")
    private val irohaQueryHelper: IrohaQueryHelper
) {

    private val gson = GsonInstance.get()

    /**
     * Return all withdrawal proofs
     * @param originalWithdrawalTxHash - original withdrawal tx hash
     * @param clientAccountId - id of account that initiated withdrawal operation
     * @return list of proofs
     */
    fun getAllProofs(
        originalWithdrawalTxHash: String,
        clientAccountId: String
    ): Result<List<EthWithdrawalProof>, Exception> {
        return irohaQueryHelper.getAccountDetailsByKeyOnly(
            clientAccountId, originalWithdrawalTxHash
        ).map { details ->
            val proofs = ArrayList<EthWithdrawalProof>()
            details.forEach { detail ->
                val proofSetter = detail.key
                val isRealProofSetter = ethSpecificConfig.ethWithdrawalProofSetters.any { it == proofSetter }
                if (isRealProofSetter) {
                    val proof = gson.fromJson(detail.value, EthWithdrawalProof::class.java)
                    proofs.add(proof)
                } else {
                    logger.warn("Very suspicious. $proofSetter is not a real proof setter. Please check configs.")
                }
            }
            proofs
        }
    }

    /**
     * Checks if enough proofs were collected
     * @param proofs - proofs to check
     * @return true if enough, false otherwise
     */
    fun enoughProofs(proofs: List<EthWithdrawalProof>): Result<Boolean, Exception> {
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
