package com.d3.commons.config

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BitcoinConfigTest {

    /**
     * @given config with one host
     * @when hosts are extracted
     * @then only one host is extracted
     */
    @Test
    fun testExtractHostsOneHost() {
        val bitcoinConfig = mock<BitcoinConfig> {
            on { hosts } doReturn "123"
        }
        val hosts = BitcoinConfig.extractHosts(bitcoinConfig)
        assertEquals(1, hosts.size)
        assertEquals("123", hosts[0])
    }

    /**
     * @given config with two hosts
     * @when hosts are extracted
     * @then two hosts are extracted
     */
    @Test
    fun testExtractHostsFewHosts() {
        val bitcoinConfig = mock<BitcoinConfig> {
            on { hosts } doReturn "123,456"
        }
        val hosts = BitcoinConfig.extractHosts(bitcoinConfig)
        assertEquals(2, hosts.size)
        assertEquals("123", hosts[0])
        assertEquals("456", hosts[1])
    }


    /**
     * @given config with two hosts and extra space
     * @when hosts are extracted
     * @then two hosts are extracted
     */
    @Test
    fun testExtractHostsExtraSpaces() {
        val bitcoinConfig = mock<BitcoinConfig> {
            on { hosts } doReturn "123 , 456"
        }
        val hosts = BitcoinConfig.extractHosts(bitcoinConfig)
        assertEquals(2, hosts.size)
        assertEquals("123", hosts[0])
        assertEquals("456", hosts[1])
    }

}
