/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger.exception

class AssetNotFoundException : Exception {
    constructor(message: String, ex: Exception?) : super(message, ex)
    constructor(message: String) : super(message)
    constructor(ex: Exception) : super(ex)
    constructor()
}

class TooMuchAssetVolumeException : Exception {
    constructor(message: String, ex: Exception?) : super(message, ex)
    constructor(message: String) : super(message)
    constructor(ex: Exception) : super(ex)
    constructor()
}

class TooLittleAssetVolumeException : Exception {
    constructor(message: String, ex: Exception?) : super(message, ex)
    constructor(message: String) : super(message)
    constructor(ex: Exception) : super(ex)
    constructor()
}
