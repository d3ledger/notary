/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger.context

import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.exchange.exchanger.strategy.CurveRateStrategy
import com.d3.exchange.exchanger.util.TradingPairsHelper

/**
 * [ExchangerContext] implementation that uses [CurveRateStrategy]
 */
class CurveExchangerContext(
    irohaConsumer: IrohaConsumer,
    queryHelper: IrohaQueryHelper,
    curveRateStrategy: CurveRateStrategy,
    liquidityProviderAccounts: List<String>,
    tradingPairsHelper: TradingPairsHelper
) : ExchangerContext(
    irohaConsumer,
    queryHelper,
    curveRateStrategy,
    liquidityProviderAccounts,
    tradingPairsHelper,
    irohaConsumer.creator
)
