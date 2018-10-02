package healthcheck

interface HealthyServiceInitializer {
    fun isHealthy(): Boolean
}
