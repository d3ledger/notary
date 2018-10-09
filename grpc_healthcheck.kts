#!/usr/bin/env kscript

/**
 * Checks if grpc service is ready to accept connections
 * Was designed to check if iroha is up
 * Usage: python3 iroha_healthcheck.py <host> <port>
 * Possible outputs (stdout):
 * -1: some arguments are missing
 * 0: iroha is down
 * 1: iroha is up
 * 2: unknown error
 */

@file:DependsOn("io.grpc:grpc-netty:1.14.0")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.21")

import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking

if (args.size < 2) {
    println(-1)
    System.exit(0)
}

val host = args[0]
val port = args[1].toInt()

val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()

var state = channel.getState(true)

while (state == io.grpc.ConnectivityState.IDLE || state == io.grpc.ConnectivityState.CONNECTING) {
    state = channel.getState(false)
    runBlocking {
        delay(10)
    }
}

val result = when (state) {
    io.grpc.ConnectivityState.TRANSIENT_FAILURE -> 0
    io.grpc.ConnectivityState.READY -> 1
    else -> 2
}

println(result)
