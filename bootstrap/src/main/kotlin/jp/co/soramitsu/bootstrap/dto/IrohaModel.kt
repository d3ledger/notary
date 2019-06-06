/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap.dto

import java.security.KeyPair
import javax.xml.bind.DatatypeConverter

data class IrohaAccountDto(
    val name: String = "",
    val domainId: String = "",
    val creds: List<BlockchainCreds> = listOf()
)

data class IrohaAccount(val title: String, val domain: String, val keys: HashSet<KeyPair>) :
    DtoFactory<IrohaAccountDto> {
    override fun getDTO(): IrohaAccountDto {
        val credsList = keys.map {
            BlockchainCreds(
                DatatypeConverter.printHexBinary(it.private.encoded),
                DatatypeConverter.printHexBinary(it.public.encoded)
            )
        }
        return IrohaAccountDto(this.title, domain, credsList)
    }
}

data class IrohaConfig(val host: String = "", val port: Int = 0)
