/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain

/**
 * Class extracts [SideChainEvent] received from side chain blocks
 */
interface ChainHandler<Block> {

    /**
     * Parse block and find interesting transactions.
     * @return observable with emitted chain events
     */
    fun parseBlock(block: Block): List<SideChainEvent>
}
