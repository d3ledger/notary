/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("LongevityMain")

package longevity

/**
 * Entry point for Registration Service
 */
fun main(args: Array<String>) {
    val longevityTest = LongevityTest()
    longevityTest.run()
}
