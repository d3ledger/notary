/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.notary

import java.math.BigInteger

/**
 * Class represents [Notary] intention to [sidechain.iroha.consumer.IrohaConsumer] to add transaction
 * @param creator account id of transaction creator
 * @param createdTime - time of transaction creation
 * @param quorum - tx quorum
 * @param commands commands to be sent to Iroha
 */
data class IrohaTransaction(
    val creator: String,
    val createdTime: BigInteger,
    val quorum: Int?,
    val commands: List<IrohaCommand>
) {
    constructor(
        creator: String,
        createdTime: BigInteger,
        commands: List<IrohaCommand>
    ) : this(creator, createdTime, null, commands)
}

