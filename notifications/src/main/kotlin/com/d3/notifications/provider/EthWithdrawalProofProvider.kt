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
import org.web3j.crypto.ECDSASignature
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
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
     * @return map full of proofs in form (Eth address of notary->Proof)
     */
    fun getAllProofs(
        proofStorageAccount: String
    ): Result<Map<EthNotaryAddress, EthWithdrawalProof>, Exception> {
        return irohaQueryHelper.getAccountDetails(
            storageAccountId = proofStorageAccount,
            writerAccountId = notificationsConfig.ethWithdrawalProofSetter
        ).map { details ->
            val proofs = HashMap<String, EthWithdrawalProof>()
            details.forEach { detail ->
                val proof = gson.fromJson(detail.value, EthWithdrawalProof::class.java)
                proofs[detail.key] = proof
            }
            proofs
        }
    }

    /**
     * Checks if enough proofs were collected
     * @param proofs - proofs to check
     * @return true if enough, false otherwise
     */
    fun enoughProofs(proofs: Map<EthNotaryAddress, EthWithdrawalProof>): Result<Boolean, Exception> {
        if (proofs.isEmpty()) {
            return Result.of(false)
        }
        val validProofs = proofs.count { isValidProof(it.key, it.value) }
        return irohaQueryHelper.getPeersCount().map { peers ->
            val superMajority = ((peers * 2) / 3) + 1
            validProofs >= superMajority
        }
    }

    /**
     * Check notary signature
     * @param ethNotaryAddress - Ethereum address of notary (key in setAccountDetails)
     * @param withdrawalProof - notary proof for withdrawal (value in setAccountDetail)
     * @return true if signature is correct
     */
    fun isValidProof(ethNotaryAddress: EthNotaryAddress, withdrawalProof: EthWithdrawalProof): Boolean {
        val hash = Hash.sha3(
            withdrawalProof.tokenContractAddress.replace("0x", "")
                    + String.format("%064x", BigInteger(withdrawalProof.amount)).replace("0x", "")
                    + withdrawalProof.beneficiary.replace("0x", "")
                    + withdrawalProof.irohaHash.replace("0x", "")
                    + withdrawalProof.beneficiary.replace("0x", "")
        )
        val dat = Numeric.hexStringToByteArray(hash)

        // Add Ethereum signature format
        val message = Hash.sha3(("\u0019Ethereum Signed Message:\n" + (dat.size)).toByteArray() + dat)

        val sig =
            ECDSASignature(
                BigInteger(withdrawalProof.signature.r, 16),
                BigInteger(withdrawalProof.signature.s, 16)
            )
        // there are 4 possible outcomes that should be checked with actual signatory address
        for (i in 0..3) {
            // null is a valid result, skip it
            val res = Sign.recoverFromSignature(i, sig, message)
                ?: continue
            if (Keys.getAddress(res) == ethNotaryAddress.replace("0x", ""))
                return true
        }
        return false
    }

    companion object : KLogging()
}

// Just for clarity
typealias EthNotaryAddress = String

/**
 * Data class that represents withdrawal proof details
 */
data class EthWithdrawalProof(
    val tokenContractAddress: String,
    val amount: String,
    val accountId: String,
    val irohaHash: String,
    val relay: String,
    val signature: VRSSignature,
    val beneficiary: String
)

data class VRSSignature(val v: String, val r: String, val s: String)
