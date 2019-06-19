/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.registration

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
    private val registrationConfig: NotaryRegistrationConfig,
    private val registrationStrategy: RegistrationStrategy
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
