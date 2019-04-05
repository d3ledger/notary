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