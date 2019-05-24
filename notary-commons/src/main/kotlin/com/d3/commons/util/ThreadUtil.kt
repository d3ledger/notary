/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.util

import mu.KLogging
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

private val log = KLogging().logger

/**
 * Creates thread factory that may be used in thread pool.
 * Format is [serviceName:purpose:th-(thread number):id-(thread id)]
 * @param serviceName - name of service (withdrawal, deposit and etc)
 * @param purpose - purpose of thread (Iroha listener, Ethereum tx listener and etc)
 * @return thread factory
 */
fun namedThreadFactory(
    serviceName: String,
    purpose: String
): ThreadFactory {
    return object : ThreadFactory {
        private val threadCounter = AtomicInteger(0)
        override fun newThread(runnable: Runnable): Thread {
            val thread = Executors.defaultThreadFactory().newThread(runnable)
            thread.name =
                "$serviceName:$purpose:th-${threadCounter.getAndIncrement()}:id-${thread.id}"
            return thread
        }
    }
}

/**
 * Creates pretty named single threaded pool
 * @param serviceName - name of service (withdrawal, deposit and etc)
 * @param purpose - purpose of thread (Iroha listener, Ethereum tx listener and etc)
 * @return pretty thread pool
 */
fun createPrettySingleThreadPool(
    serviceName: String,
    purpose: String
): ExecutorService {
    return Executors.newSingleThreadExecutor(namedThreadFactory(serviceName, purpose))!!
}

/**
 * Creates pretty named fixed thread pool
 * @param serviceName - name of service (withdrawal, deposit and etc)
 * @param purpose - purpose of thread (Iroha listener, Ethereum tx listener and etc)
 * @return pretty thread pool
 */
fun createPrettyFixThreadPool(
    serviceName: String,
    purpose: String
): ExecutorService {
    return Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors(),
        namedThreadFactory(serviceName, purpose)
    )!!
}

/**
 * Creates pretty named scheduled thread pool
 * @param serviceName - name of service (withdrawal, deposit and etc)
 * @param purpose - purpose of thread (Iroha listener, Ethereum tx listener and etc)
 * @return pretty thread pool
 */
fun createPrettyScheduledThreadPool(
    serviceName: String,
    purpose: String
): ScheduledExecutorService {
    return Executors.newScheduledThreadPool(
        Runtime.getRuntime().availableProcessors(),
        namedThreadFactory(serviceName, purpose)
    )!!
}

