/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package d3.report.filter;

import d3.report.model.ReportItem;

import java.util.ArrayList;
import java.util.List;


/**
 * Class for filtering tests and test cases
 */
public class TestsFilter {

    /**
     * Collects test cases with no description
     *
     * @param testCases   list full of test cases.
     * @param reportItems test cases with description
     * @return test cases with no description
     */
    public List<String> getNoDescriptionTests(List<String> testCases, List<ReportItem> reportItems) {
        List<String> noDescriptionCases = new ArrayList<>();
        for (String testCase : testCases) {
            if (!testCaseIsWellReported(testCase, reportItems)) {
                noDescriptionCases.add(testCase);
            }
        }
        return noDescriptionCases;
    }

    private boolean testCaseIsWellReported(String testCase, List<ReportItem> reportItems) {
        for (ReportItem reportItem : reportItems) {
            if (reportItem.getTestCaseName().equals(testCase)) {
                return true;
            }
        }
        return false;
    }
}
