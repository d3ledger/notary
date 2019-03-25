/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.notary.endpoint

import com.d3.commons.notary.endpoint.ServerInitializationBundle
import com.d3.eth.deposit.endpoint.*
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.squareup.moshi.Moshi
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger

/**
 * Fixture for testing notary.endpoint
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RefundServerEndpointTest {

    /** Server initialization bundle */
    val serverBundle = ServerInitializationBundle(8080, "eth")

    /** JSON adapter */
    val moshi =
        Moshi.Builder().add(EthNotaryResponseMoshiAdapter()).add(
            BigInteger::class.java,
            BigIntegerMoshiAdapter()
        )
            .build()

    /** Stub for Ethereum refund request, should represent tx hash from Iroha */
    private val ethRequest = EthRefundRequest("tx_hash_from_iroha")

    /** Successful response */
    private val successResponse = EthNotaryResponse.Successful(
        "signature",
        EthRefund("address", "coin", "10", "irohaTxHash", "")
    )

    /** Strategy mock that always returns success */
    private val ethRefundStrategyMock = mock<EthRefundStrategy> {
        val request = any<EthRefundRequest>()
        on {
            performRefund(request)
        } doReturn successResponse
    }

    val server = RefundServerEndpoint(serverBundle, ethRefundStrategyMock)

    /**
     * @given initialized server class
     * @when  call onCallEthRefund()
     * @then  check that answer returns success
     */
    @Test
    fun onEthRefundCallTest() {
        val request = moshi.adapter(EthRefundRequest::class.java).toJson(ethRequest)
        val result = server.onCallEthRefund(request)

        assertEquals(HttpStatusCode.OK, result.code)
        assertEquals(successResponse, moshi.adapter(EthNotaryResponse::class.java).fromJson(result.message))
    }

    /**
     * @given initialized server class
     * @when  call onCallEthRefund() with null parameter
     * @then  check that answer returns key not found
     */
    @Test
    fun emptyCall() {
        val failureResponse = EthNotaryResponse.Error("Request has been failed. Error in URL")

        val refundAnswer = server.onCallEthRefund(null)

        assertEquals(HttpStatusCode.BadRequest, refundAnswer.code)
        assertEquals(failureResponse.reason, refundAnswer.message)
    }
}
