/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap.iroha

import com.google.common.util.concurrent.ThreadFactoryBuilder
import jp.co.soramitsu.bootstrap.dto.IrohaConfig
import jp.co.soramitsu.iroha.java.IrohaAPI
import org.springframework.stereotype.Component
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Lazy Iroha API pool
 */
@Component
class LazyIrohaAPIPool : Closeable {

    // Collection of IrohaAPIs
    private val apiPool = HashMap<IrohaConfig, IrohaAPI>()
    // Executor service that manages old and stale IrohaAPIs
    private val scavengerExecutorService = Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder().setNameFormat("iroha-api-scavenger").build()
    )

    init {
        // Removes old APIs every 5 minutes
        scavengerExecutorService.scheduleWithFixedDelay({ scavenge() }, 5, 5, TimeUnit.MINUTES)
    }

    /**
     * Creates(if needed) and returns IrohaAPI
     * @param irohaConfig - configuration of IrohaAPI
     * @return irohaAPI with given configurations
     */
    @Synchronized
    fun getApi(irohaConfig: IrohaConfig): IrohaAPI {
        apiPool[irohaConfig]?.let { api ->
            // Check if connection is available
            if (isAvailableAPI(api)) {
                return api
            }
        }
        val api = IrohaAPI(irohaConfig.host, irohaConfig.port)
        apiPool[irohaConfig] = api
        return api
    }

    /**
     * Clears apiPool from stale and closed APIs
     */
    @Synchronized
    private fun scavenge() {
        val aliveAPIs = apiPool.filter { api -> isAvailableAPI(api.value) }
        apiPool.clear()
        apiPool.putAll(aliveAPIs)
    }

    /**
     * Cheks if IrohaAPI is available
     * @param api - api to check
     * @return true if available to use
     */
    private fun isAvailableAPI(api: IrohaAPI) = !api.channel.isTerminated && !api.channel.isShutdown

    @Synchronized
    override fun close() {
        apiPool.values.forEach { api ->
            api.close()
        }
        scavengerExecutorService.shutdownNow()
    }
}
