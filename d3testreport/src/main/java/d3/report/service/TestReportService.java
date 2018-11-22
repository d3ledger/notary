package d3.report.service;

import d3.report.counter.TestsCounter;
import d3.report.filter.TestsFilter;
import d3.report.model.*;
import d3.report.parser.TestParser;
import d3.report.reader.CodeReader;
import d3.report.walker.TestsWalker;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*
    Report creating service
 */
public class TestReportService {
    private static final String DATE_FORMAT = "dd.MM.yyyy hh:mm";
    private static final String D3_PROJECT_TITLE = "D3 test report";

    private final TestParser testParser = new TestParser();
    private final TestsWalker testsWalker = new TestsWalker();
    private final TestsCounter testsCounter = new TestsCounter();
    private final TestsFilter testsFilter = new TestsFilter();
    private final CodeReader codeReader = new CodeReader();

    /**
     * Creates complete report for all test files located in given folder
     *
     * @param rootFolder root folder where all the tests are stored
     * @return complete report
     */
    public Report create(String rootFolder) throws IOException {
        Report report = new Report();
        List<String> testFiles = testsWalker.getTestFiles(rootFolder);
        Summary summary = initSummary(rootFolder);
        report.setSummary(summary);
        if (testFiles.isEmpty()) {
            return report;
        }
        List<String> testFilesPretty = new ArrayList<>(testFiles.size());
        List<TestFileReport> reportData = new ArrayList<>();
        List<String> noDescriptionTestCases = new ArrayList<>();
        int testCasesCount = 0;
        int disabledCasesCount = 0;
        for (String file : testFiles) {
            TestCount testCount = enrichReport(
                    file,
                    rootFolder,
                    testFilesPretty,
                    reportData,
                    noDescriptionTestCases);
            testCasesCount += testCount.getTestCases();
            disabledCasesCount += testCount.getDisabledTestCases();
        }
        summary.setFilesWithTests(testFilesPretty.size());
        summary.setTestCases(testCasesCount);
        summary.setDisabledCases(disabledCasesCount);
        report.setReportData(reportData);
        report.setTestFiles(testFilesPretty);
        report.setNoDescriptionTestCases(noDescriptionTestCases);
        summary.setLacksInDescription(noDescriptionTestCases.size());
        return report;
    }

    private TestCount enrichReport(String file,
                                   String rootFolder,
                                   List<String> testFiles,
                                   List<TestFileReport> reportData,
                                   List<String> noDescriptionTestCases) throws IOException {
        List<String> linesOfCode = codeReader.readLines(file);
        int testCases = testsCounter.countTests(linesOfCode);
        if (testCases == 0) {
            return TestCount.noTests();
        }
        List<ReportItem> reportItems = testParser.createReport(linesOfCode);
        List<String> allTestCases = testParser.getTestCases(linesOfCode);
        noDescriptionTestCases.addAll(testsFilter.getNoDescriptionTests(allTestCases, reportItems));
        String testFileName = prettifyFileName(file, rootFolder);
        testFiles.add(testFileName);
        reportData.add(new TestFileReport(testFileName, reportItems));
        return new TestCount()
                .setDisabledTestCases(testsCounter.countDisabledTests(reportItems))
                .setTestCases(testCases);
    }

    private Summary initSummary(String rootFolder) {
        Summary summary = new Summary();
        summary.setGenerationDate(getCurrentDate());
        summary.setProjectRootDir(rootFolder);
        summary.setTitle(D3_PROJECT_TITLE);
        return summary;
    }

    private String prettifyFileName(String filePath, String rootFolder) {
        return filePath.substring(filePath.indexOf(rootFolder) + rootFolder.length());
    }

    private String getCurrentDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
        return simpleDateFormat.format(new Date());
    }
}
