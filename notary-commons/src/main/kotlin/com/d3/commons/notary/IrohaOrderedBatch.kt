/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.notary

/**
 * Class represents [Notary] intention to [sidechain.iroha.consumer.IrohaConsumer] to add batch transaction
 * @param transactions - transactions in the batch
 */
data class IrohaOrderedBatch(val transactions: List<IrohaTransaction>)
