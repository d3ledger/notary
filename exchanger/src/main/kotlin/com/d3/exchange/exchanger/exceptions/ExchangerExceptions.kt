/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger.exceptions

class AssetNotFoundException(message: String, ex: Exception?) : Exception(message, ex)

class TooMuchAssetVolumeException(message: String) : Exception(message)

class TooLittleAssetVolumeException(message: String) : Exception(message)

class UnsupportedTradingPairException(message: String) : Exception(message)
