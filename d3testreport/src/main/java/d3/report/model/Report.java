/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package d3.report.model;

import java.util.List;

/*
 * Main report class
 */
public class Report {

  // Brief information: total amount of test cases, disabled tests and etc
  private Summary summary;
  // Test files
  private List<String> testFiles;
  // Report data with more detailed information divided by test files
  private List<TestFileReport> reportData;
  // Test cases with no description
  private List<String> noDescriptionTestCases;

  public Summary getSummary() {
    return summary;
  }

  public void setSummary(Summary summary) {
    this.summary = summary;
  }

  public List<String> getTestFiles() {
    return testFiles;
  }

  public void setTestFiles(List<String> testFiles) {
    this.testFiles = testFiles;
  }

  public List<TestFileReport> getReportData() {
    return reportData;
  }

  public void setReportData(List<TestFileReport> reportData) {
    this.reportData = reportData;
  }

  public List<String> getNoDescriptionTestCases() {
    return noDescriptionTestCases;
  }

  public void setNoDescriptionTestCases(List<String> noDescriptionTestCases) {
    this.noDescriptionTestCases = noDescriptionTestCases;
  }

  public boolean hasLacksOfDescription() {
    return noDescriptionTestCases != null && !noDescriptionTestCases.isEmpty();
  }
}
