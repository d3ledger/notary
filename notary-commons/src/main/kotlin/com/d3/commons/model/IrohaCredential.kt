/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.model

import com.d3.commons.config.IrohaCredentialRawConfig
import jp.co.soramitsu.iroha.java.Utils
import java.security.KeyPair

/**
 * Class that represents account in Iroha
 * @param accountId - full id in Iroha: account name + domain name
 * @param keyPair - iroha Keypair associated with [accountId]
 */
data class IrohaCredential(
    val accountId: String,
    val keyPair: KeyPair
) {
    /**
     * Create credential from config
     */
    constructor(credentialConfig: IrohaCredentialRawConfig) : this(
        credentialConfig.accountId,
        Utils.parseHexKeypair(
            credentialConfig.pubkey,
            credentialConfig.privkey
        )
    )
}
