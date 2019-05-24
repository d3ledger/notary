/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.registration

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialConfig
import com.d3.commons.config.IrohaCredentialRawConfig

/**
 * Interface represents configs for registration service for cfg4k
 */
interface NotaryRegistrationConfig {

    /** Registration service port */
    val port: Int

    /** Iroha configs */
    val iroha: IrohaConfig

    /** Iroha registration service credential */
    val registrationCredential: IrohaCredentialRawConfig

    /** Iroha account to store clients */
    val clientStorageAccount: String

    /** Iroha account of validator service */
    val brvsAccount: String

    /** Fake pubkey to register user accounts with first**/
    val primaryPubkey: String

    /** Fake privkey conforming to the pub one to register user accounts with first**/
    val primaryPrivkey: String
}
