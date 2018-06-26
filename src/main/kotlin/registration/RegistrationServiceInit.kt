package registration

import com.github.kittinunf.result.Result
import main.ConfigKeys
import notary.CONFIG


/**
 * Initialisation of Registration Service
 */
class RegistrationServiceInit {

    /**
     * Init Registration Service
     */
    fun init(): Result<Unit, Exception> {
        return Result.of {
            // init iroha listener
            // init iroha consumer
            initHttpEndpoint()
            Unit
        }
    }

    /**
     * Init Registration Service endpoint
     */
    private fun initHttpEndpoint() {
        RegistrationServiceEndpoint(CONFIG[ConfigKeys.registrationPort])
    }
}
