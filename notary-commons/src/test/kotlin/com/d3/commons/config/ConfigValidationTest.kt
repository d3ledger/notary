/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.config

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ConfigValidationTest {

    /**
     * @given failing validation rule
     * @when configuration is loaded with given failing rule
     * @then it fails with IllegalArgumentException
     */
    @Test
    fun testLoadConfigsFailedValidation() {
        val validationRule = mock<ConfigValidationRule<TestConfig>> {
            on { validate(any()) } doThrow IllegalArgumentException()
        }
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            loadConfigs(
                "test",
                TestConfig::class.java,
                "/test.properties",
                validationRule
            ).get()
        }
    }

    /**
     * @given two rules: succeeding and failing
     * @when configuration is loaded with given rules
     * @then it fails with IllegalArgumentException
     */
    @Test
    fun testLoadConfigsFailedSecondValidation() {
        val validationRuleSuccess = mock<ConfigValidationRule<TestConfig>>()
        val validationRuleFail = mock<ConfigValidationRule<TestConfig>> {
            on { validate(any()) } doThrow IllegalArgumentException()
        }
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            loadConfigs(
                "test",
                TestConfig::class.java,
                "/test.properties",
                validationRuleSuccess, validationRuleFail
            ).get()
        }
    }

    /**
     * @given succeeding rule
     * @when configuration is loaded with given rule
     * @then it doesn't throw any exception
     */
    @Test
    fun testLoadConfigsValidationSuccess() {
        val validationRuleSuccess = mock<ConfigValidationRule<TestConfig>>()
        val config = loadConfigs(
            "test",
            TestConfig::class.java,
            "/test.properties",
            validationRuleSuccess
        ).get()
        assertEquals("test@notary", config.testCredentialConfig.accountId)
    }

    /**
     * @given no rules
     * @when configuration is loaded with no rules
     * @then it doesn't throw any exception
     */
    @Test
    fun testLoadConfigsNoValidationSuccess() {
        val config = loadConfigs(
            "test",
            TestConfig::class.java,
            "/test.properties"
        ).get()
        assertEquals("test@notary", config.testCredentialConfig.accountId)
    }
}
