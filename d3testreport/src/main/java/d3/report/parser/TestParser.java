package d3.report.parser;

import d3.report.model.ReportItem;

import java.util.ArrayList;
import java.util.List;

/*
    Class that is used to parse tests source code
 */
public class TestParser {
    private static final String GIVEN = "@given";
    private static final String TODO = "TODO";
    private static final String WHEN = "@when";
    private static final String THEN = "@then";
    private static final String TEST = "@Test";
    private static final String TEST_INSTANCE = "@TestInstance";
    private static final String FUN = "fun ";
    private static final String CLASS = "class";
    private static final String DISABLED = "@Disabled";
    private static final String EXTRA_SYMBOLS_REG_EXP = GIVEN + "|" + WHEN + "|" + THEN + "|" + FUN + "|" + DISABLED;

    /**
     * Creates report based on test file source code
     *
     * @param linesOfCode list full of test file source code lines
     * @return test file report
     */
    public List<ReportItem> createReport(List<String> linesOfCode) {
        List<ReportItem> reportItems = new ArrayList<>();
        boolean disabledTestClass = isDisabledTestClass(linesOfCode);
        int lineToProcess = 0;
        while (lineToProcess < linesOfCode.size()) {
            lineToProcess = addReportItem(
                    reportItems,
                    linesOfCode,
                    lineToProcess,
                    disabledTestClass
            );
        }
        return reportItems;
    }

    /**
     * Returns test cases based on test file source code
     *
     * @param linesOfCode list full of test file source code lines
     * @return test cases
     */
    public List<String> getTestCases(List<String> linesOfCode) {
        List<String> testCases = new ArrayList<>();
        int lineToProcess = 0;
        while (lineToProcess < linesOfCode.size()) {
            lineToProcess = addTestCase(testCases, linesOfCode, lineToProcess);
        }
        return testCases;
    }

    private boolean isDisabledTestClass(List<String> linesOfCode) {
        String line;
        for (int lineToProcess = 0; lineToProcess < linesOfCode.size(); lineToProcess++) {
            line = linesOfCode.get(lineToProcess);
            if (!line.startsWith(CLASS)) {
                continue;
            }
            return codeContains(linesOfCode, lineToProcess, DISABLED);
        }
        return false;
    }

    private boolean codeContains(List<String> lineOfCode, int lastLine, String code) {
        for (int i = 0; i < lastLine; i++) {
            if (lineOfCode.get(i).contains(code)) {
                return true;
            }
        }
        return false;
    }

    private int addReportItem(List<ReportItem> reportItems, List<String> linesOfCode, int lineToProcess, boolean disabledTestClass) {
        String line;
        for (; lineToProcess < linesOfCode.size(); lineToProcess++) {
            line = linesOfCode.get(lineToProcess);
            if (!line.contains(GIVEN)) {
                continue;
            }
            StringBuilder givenContent = new StringBuilder();
            while (!line.contains(WHEN)) {
                givenContent.append(line);
                if (++lineToProcess == linesOfCode.size()) {
                    return lineToProcess;
                }
                line = linesOfCode.get(lineToProcess);
            }
            StringBuilder whenContent = new StringBuilder();
            while (!line.contains(THEN)) {
                whenContent.append(line);
                if (++lineToProcess == linesOfCode.size()) {
                    return lineToProcess;
                }
                line = linesOfCode.get(lineToProcess);
            }
            StringBuilder thenContent = new StringBuilder();
            boolean disabled = false;
            while (!isTestLine(line)) {
                if (line.contains(DISABLED)) {
                    disabled = true;
                } else if (!line.contains(TODO)) {
                    thenContent.append(line);
                }
                if (++lineToProcess == linesOfCode.size()) {
                    return lineToProcess;
                }
                line = linesOfCode.get(lineToProcess);
            }
            while (!line.contains(FUN)) {
                if (line.contains(DISABLED)) {
                    disabled = true;
                }
                if (++lineToProcess == linesOfCode.size()) {
                    return lineToProcess;
                }
                line = linesOfCode.get(lineToProcess);
            }
            int testLine = lineToProcess;
            ReportItem reportItem = new ReportItem()
                    .setGiven(removeExtraSymbols(givenContent.toString()))
                    .setThen(removeExtraSymbols(thenContent.toString()))
                    .setWhen(removeExtraSymbols(whenContent.toString()))
                    .setTestCaseName(removeExtraSymbols(line))
                    .setDisabled(disabled || disabledTestClass)
                    .setLine(testLine + 1);
            reportItems.add(reportItem);
        }
        return ++lineToProcess;
    }

    private int addTestCase(List<String> testCases, List<String> linesOfCode, int lineToProcess) {
        String line;
        for (; lineToProcess < linesOfCode.size(); lineToProcess++) {
            line = linesOfCode.get(lineToProcess);
            if (!isTestLine(line)) {
                continue;
            }
            while (!line.contains(FUN)) {
                if (++lineToProcess == linesOfCode.size()) {
                    return lineToProcess;
                }
                line = linesOfCode.get(lineToProcess);
            }
            testCases.add(removeExtraSymbols(line));
        }
        return ++lineToProcess;
    }

    private boolean isTestLine(String line) {
        return line.contains(TEST) && !line.contains(TEST_INSTANCE);
    }

    private String removeExtraSymbols(String line) {
        return line
                .replaceAll(EXTRA_SYMBOLS_REG_EXP, "")
                .replaceAll("[{}]", "")
                .replaceAll("\\*/", "")
                .replaceAll("\\*", "")
                .replaceAll("      ", " ")
                .trim();
    }
}
