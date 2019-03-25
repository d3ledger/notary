/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package d3.report.model;

/*
    Report data of single test case
 */
public class ReportItem {
    //Number of line of code where test case appears
    private int line;
    // Name of test case(function name)
    private String testCaseName;
    private String given;
    private String when;
    private String then;
    private boolean disabled;

    public int getLine() {
        return line;
    }

    public ReportItem setLine(int line) {
        this.line = line;
        return this;
    }

    public String getTestCaseName() {
        return testCaseName;
    }

    public ReportItem setTestCaseName(String testCaseName) {
        this.testCaseName = testCaseName;
        return this;
    }

    public String getGiven() {
        return given;
    }

    public ReportItem setGiven(String given) {
        this.given = given;
        return this;
    }

    public String getWhen() {
        return when;
    }

    public ReportItem setWhen(String when) {
        this.when = when;
        return this;
    }

    public String getThen() {
        return then;
    }

    public ReportItem setThen(String then) {
        this.then = then;
        return this;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public ReportItem setDisabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }
}
