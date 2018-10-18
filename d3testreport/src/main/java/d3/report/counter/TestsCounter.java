package d3.report.counter;

import d3.report.model.ReportItem;

import java.util.List;

/*
    Class that takes care about counting things.
 */
public class TestsCounter {
    private static final String TEST_ANNOTATION = "@Test";
    private static final String TEST_INSTANCE_ANNOTATION = "@TestInstance";

    /**
     * Counts test cases
     *
     * @param linesOfCode lines of code to count test cases
     * @return amount of test cases in code
     */
    public int countTests(List<String> linesOfCode) {
        int tests = 0;
        for (String line : linesOfCode) {
            if (line.contains(TEST_ANNOTATION) && !line.contains(TEST_INSTANCE_ANNOTATION)) {
                tests++;
            }
        }
        return tests;
    }

    /**
     * Counts disabled test cases
     *
     * @param reportItems report items to count disabled test cases
     * @return amount of disabled test cases in report items
     */
    public int countDisabledTests(List<ReportItem> reportItems) {
        int disabledTests = 0;
        for (ReportItem reportItem : reportItems) {
            if (reportItem.isDisabled()) {
                disabledTests++;
            }
        }
        return disabledTests;
    }
}
