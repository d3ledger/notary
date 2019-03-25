/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package d3.report.model;

import java.util.List;

/*
    Class that bounds test file name with report
 */
public class TestFileReport {
    private String fileName;
    private List<ReportItem> reportItems;

    //For json serialization
    public TestFileReport() {

    }

    public TestFileReport(String fileName, List<ReportItem> reportItems) {
        this.fileName = fileName;
        this.reportItems = reportItems;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<ReportItem> getReportItems() {
        return reportItems;
    }

    public void setReportItems(List<ReportItem> reportItems) {
        this.reportItems = reportItems;
    }
}
