package main

import notary.NotaryInitialization
import sideChain.iroha.IrohaInitializtion

/**
 * Application entry point
 */
fun main(args: Array<String>) {
    val irohaInitializtion = IrohaInitializtion()
    irohaInitializtion.loadIrohaLibrary()
    val notary = NotaryInitialization()
    notary.init()
}
