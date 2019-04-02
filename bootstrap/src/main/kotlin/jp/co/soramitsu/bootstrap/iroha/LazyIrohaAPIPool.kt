package jp.co.soramitsu.bootstrap.iroha

import jp.co.soramitsu.bootstrap.dto.IrohaConfig
import jp.co.soramitsu.iroha.java.IrohaAPI
import org.springframework.stereotype.Component
import java.io.Closeable

/**
 * Lazy Iroha API pool
 */
@Component
class LazyIrohaAPIPool : Closeable {

    // Collection of IrohaAPIs
    private val apiPool = HashMap<IrohaConfig, IrohaAPI>()

    /**
     * Creates(if needed) and returns IrohaAPI
     * @param irohaConfig - configuration of IrohaAPI
     * @return irohaAPI with given configurations
     */
    @Synchronized
    fun getApi(irohaConfig: IrohaConfig): IrohaAPI {
        apiPool[irohaConfig]?.let { api ->
            return api
        }
        val api = IrohaAPI(irohaConfig.host, irohaConfig.port)
        apiPool[irohaConfig] = api
        return api
    }

    @Synchronized
    override fun close() {
        apiPool.values.forEach { api ->
            api.close()
        }
    }
}
