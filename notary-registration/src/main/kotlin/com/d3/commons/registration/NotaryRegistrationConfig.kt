/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.registration

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialConfig

/**
 * Interface represents configs for registration service for cfg4k
 */
interface NotaryRegistrationConfig {

    /** Registration service port */
    val port: Int

    /** Iroha configs */
    val iroha: IrohaConfig

    /** Iroha registration service credential */
    val registrationCredential: IrohaCredentialConfig

    /** Iroha account to store clients */
    val clientStorageAccount: String

    /** Iroha account of validator service */
    val brvsAccount: String

    /** Path to a fake pubkey to register user accounts with first**/
    val primaryPubkeyPath: String

    /** Path to a fake privkey conforming to the pub one to register user accounts with first**/
    val primaryPrivkeyPath: String
}
