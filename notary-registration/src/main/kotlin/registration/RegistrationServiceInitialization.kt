package registration

import com.github.kittinunf.result.Result
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Initialization of registration service
 * @param registrationConfig - configuration of registration service
 * @param registrationStrategy - startegy of registration service
 */
@Component
class RegistrationServiceInitialization(
    @Autowired private val registrationConfig: NotaryRegistrationConfig,
    @Autowired private val registrationStrategy: RegistrationStrategy
) {

    /**
     * Init registration service
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Init registration service" }
        return Result.of {
            RegistrationServiceEndpoint(
                registrationConfig.port,
                registrationStrategy
            )
            Unit
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
