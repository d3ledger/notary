package com.d3.chainadapter.provider

interface LastReadBlockProvider {
    fun getLastBlockHeight(): Long
    fun saveLastBlockHeight(height: Long)
}
