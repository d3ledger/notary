@file:JvmName("LongevityMain")

import config.IrohaCredentialConfig
import integration.helper.IntegrationHelperUtil
import sidechain.iroha.util.ModelUtil

/**
 * Entry point for Registration Service
 */
fun main(args: Array<String>) {
    val longevityTest = LongevityTest()
    longevityTest.runServices()
}
