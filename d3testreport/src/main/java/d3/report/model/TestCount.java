/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package d3.report.model;

public class TestCount {
    private int testCases;
    private int disabledTestCases;

    public static TestCount noTests() {
        return new TestCount();
    }

    public int getTestCases() {
        return testCases;
    }

    public TestCount setTestCases(int testCases) {
        this.testCases = testCases;
        return this;
    }

    public int getDisabledTestCases() {
        return disabledTestCases;
    }

    public TestCount setDisabledTestCases(int disabledTestCases) {
        this.disabledTestCases = disabledTestCases;
        return this;
    }
}

