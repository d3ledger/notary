package main

import notary.NotaryInitialization

/**
 * Load iroha binding library
 */
fun loadIrohaLibrary() {
    try {
        System.loadLibrary("irohajava")
    } catch (e: UnsatisfiedLinkError) {
        System.err.println("Native code library failed to load. \n" + e)
        System.exit(1)
    }
}

/**
 * Application entry point
 */
fun main(args: Array<String>) {
    loadIrohaLibrary()
    val notary = NotaryInitialization()
    notary.init()
}
