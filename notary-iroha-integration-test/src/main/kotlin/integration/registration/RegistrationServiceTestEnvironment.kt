/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.registration

import com.d3.commons.model.IrohaCredential
import com.d3.commons.registration.NotaryRegistrationStrategy
import com.d3.commons.registration.RegistrationServiceInitialization
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.toHexString
import integration.helper.IrohaIntegrationHelperUtil
import jp.co.soramitsu.iroha.java.Utils
import khttp.responses.Response
import java.io.Closeable

/**
 * Environment for registration service running in tests
 */
class RegistrationServiceTestEnvironment(private val integrationHelper: IrohaIntegrationHelperUtil) :
    Closeable {

    val registrationConfig =
        integrationHelper.configHelper.createRegistrationConfig(integrationHelper.accountHelper)

    private val registrationKeyPair = Utils.parseHexKeypair(
        registrationConfig.registrationCredential.pubkey,
        registrationConfig.registrationCredential.privkey
    )

    private val registrationCredentials =
        IrohaCredential(registrationConfig.registrationCredential.accountId, registrationKeyPair)

    private val irohaConsumer =
        IrohaConsumerImpl(registrationCredentials, integrationHelper.irohaAPI)

    private val primaryKeyPair = Utils.parseHexKeypair(
        registrationConfig.primaryPubkey,
        registrationConfig.primaryPrivkey
    )

    private val registrationStrategy =
        NotaryRegistrationStrategy(
            irohaConsumer,
            registrationConfig.clientStorageAccount,
            registrationConfig.brvsAccount,
            primaryKeyPair
        )

    val registrationInitialization =
        RegistrationServiceInitialization(registrationConfig, registrationStrategy)

    fun register(
        name: String,
        pubkey: String = ModelUtil.generateKeypair().public.toHexString(),
        domain: String = "d3"
    ): Response {
        return khttp.post(
            "http://127.0.0.1:${registrationConfig.port}/users",
            data = mapOf("name" to name, "pubkey" to pubkey, "domain" to domain)
        )
    }

    override fun close() {
        integrationHelper.close()
    }
}
