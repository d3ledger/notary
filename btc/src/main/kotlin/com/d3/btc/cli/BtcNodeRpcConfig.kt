package com.d3.btc.cli

interface BtcNodeRpcConfig {
    //RPC password
    val password: String
    //RPC user
    val user: String
    //Bitcoin node host name
    val host: String
    //Bitcoin node port
    val port: Int
}
