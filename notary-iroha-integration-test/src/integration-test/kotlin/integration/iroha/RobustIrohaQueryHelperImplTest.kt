/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.iroha

import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.sidechain.iroha.util.impl.RobustIrohaQueryHelperImpl
import com.d3.commons.sidechain.iroha.util.impl.RobustQueryException
import com.github.kittinunf.result.failure
import integration.helper.ContainerHelper
import integration.helper.IrohaIntegrationHelperUtil
import jp.co.soramitsu.iroha.java.ErrorResponseException
import jp.co.soramitsu.iroha.java.IrohaAPI
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import kotlin.test.fail

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RobustIrohaQueryHelperImplTest {

    private val integrationHelperUtil = IrohaIntegrationHelperUtil()

    /**
     * @given running Iroha and an instance of RobustIrohaQueryHelperImpl
     * @when getBlock(1) is called
     * @then query call is executed successfully
     */
    @Test
    fun testSuccessfulQuery() {
        RobustIrohaQueryHelperImpl(
            IrohaQueryHelperImpl(
                integrationHelperUtil.irohaAPI,
                integrationHelperUtil.accountHelper.testCredential
            ),
            totalTimeoutMls = 30_000
        ).getBlock(1).failure { ex -> fail(ex) }
    }

    /**
     * @given running Iroha and an instance of RobustIrohaQueryHelperImpl
     * @when getBlock(Long.MAX_VALUE) is called
     * @then query fails with a specific ErrorResponseException
     */
    @Test
    fun testInvalidQuery() {
        RobustIrohaQueryHelperImpl(
            IrohaQueryHelperImpl(
                integrationHelperUtil.irohaAPI,
                integrationHelperUtil.accountHelper.testCredential
            ),
            totalTimeoutMls = 30_000
        ).getBlock(Long.MAX_VALUE).fold(
            { fail() },
            { ex ->
                assertTrue(ex is ErrorResponseException)
            })
    }

    /**
     * @given running Iroha and an instance of RobustIrohaQueryHelperImpl
     * @when getBlock(1) is called while Iroha is off-line
     * @then query call is executed successfully after Iroha start
     */
    @Test
    fun testIrohaDieReviveQuery() {
        ContainerHelper().use { containerHelper ->
            // Start Iroha
            containerHelper.irohaContainer.withFixedPort(50555).start()
            val robustQueryHelper = RobustIrohaQueryHelperImpl(
                IrohaQueryHelperImpl(
                    containerHelper.irohaContainer.api,
                    integrationHelperUtil.accountHelper.testCredential
                ),
                totalTimeoutMls = 60_000
            )
            // Kill Iroha
            containerHelper.irohaContainer.postgresDockerContainer.stop()
            containerHelper.irohaContainer.irohaDockerContainer.stop()
            Thread(Runnable {
                // Delay a little bit
                Thread.sleep(5_000)
                containerHelper.irohaContainer.start()
            }).start()
            robustQueryHelper.getBlock(1).failure { ex -> fail(ex) }
        }
    }

    /**
     * @given an instance of RobustIrohaQueryHelperImpl with no Iroha being started
     * @when getBlock(1) is called
     * @then query fails with a RobustQueryException
     * */
    @Test
    fun testIrohaCompletelyDeadQuery() {

        RobustIrohaQueryHelperImpl(
            IrohaQueryHelperImpl(
                IrohaAPI("localhost", 666),
                integrationHelperUtil.accountHelper.testCredential
            ),
            totalTimeoutMls = 30_000
        ).getBlock(1).fold(
            { fail() },
            { ex ->
                assertTrue(ex is RobustQueryException && ex.retryAttempts >= 5)
            })
    }
}
