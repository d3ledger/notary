/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

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
    private static final String DATE_FORMAT = "dd.MM.yyyy HH:mm";
    private static final String D3_PROJECT_TITLE = "D3 test report";

    private final TestParser testParser = new TestParser();
    private final TestsWalker testsWalker = new TestsWalker();
    private final TestsCounter testsCounter = new TestsCounter();
    private final TestsFilter testsFilter = new TestsFilter();
    private final CodeReader codeReader = new CodeReader();


    /**
     * Creates complete report for all test files located in given folders
     *
     * @param rootFolders root folder where all the tests are stored
     * @return complete report
     */
    public Report create(List<String> rootFolders) throws IOException {
        List<Report> reports = new ArrayList<>();
        for (String rootFolder : rootFolders) {
            reports.add(create(rootFolder));
        }
        return merge(reports);
    }

    /**
     * Creates complete report for all test files located in given folder
     *
     * @param rootFolder root folder where all the tests are stored
     * @return complete report
     */
    private Report create(String rootFolder) throws IOException {
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


    //Merges reports into one
    private Report merge(List<Report> reports) {
        if (reports == null || reports.isEmpty()) {
            throw new IllegalArgumentException("Cannot merge reports, because there is nothing to merge");
        } else if (reports.size() == 1) {
            return reports.get(0);
        }
        Report mainReport = reports.get(0);
        Summary mainReportSummary = mainReport.getSummary();
        for (int i = 1; i < reports.size(); i++) {
            Report report = reports.get(i);
            mainReport.getTestFiles().addAll(report.getTestFiles());
            mainReport.getNoDescriptionTestCases().addAll(report.getNoDescriptionTestCases());
            mainReport.getReportData().addAll(report.getReportData());

            Summary summary = report.getSummary();
            mainReportSummary.setDisabledCases(mainReportSummary.getDisabledCases() + summary.getDisabledCases());
            mainReportSummary.setFilesWithTests(mainReportSummary.getFilesWithTests() + summary.getFilesWithTests());
            mainReportSummary.setLacksInDescription(mainReportSummary.getLacksInDescription() + summary.getLacksInDescription());
            mainReportSummary.setTestCases(mainReportSummary.getTestCases() + summary.getTestCases());
        }
        return mainReport;
    }
}
