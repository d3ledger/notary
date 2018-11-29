#!/usr/bin/env kscript

/**
 * Checks if grpc service is ready to accept connections
 * Was designed to check if iroha is up
 * Usage: ./grpc_healthcheck <host> <port>
 * Possible exit codes:
 * 0: iroha is up
 * 1: iroha is down
 * 2: unknown error
 * 3: some arguments are missing
 */

@file:DependsOn("io.grpc:grpc-netty:1.14.0")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.21")

import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

if (args.size < 2) {
    System.exit(3)
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
    io.grpc.ConnectivityState.READY -> 0
    io.grpc.ConnectivityState.TRANSIENT_FAILURE -> 1
    else -> 2
}

System.exit(result)
