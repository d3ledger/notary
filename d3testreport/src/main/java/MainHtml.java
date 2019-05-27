/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import d3.report.enums.TestFolder;
import d3.report.service.TestReportService;

import java.io.*;

/*
 * Main class to create pretty looking HTML reports
 */
public class MainHtml {

  private static final TestReportService REPORT_SERVICE = new TestReportService();
  //Report template. See http://mustache.github.io
  private static final String REPORT_TEMPLATE_FILE = "report.html";
  //Folder where the generated HTML file will be located
  private static final String REPORT_FOLDER = "d3testreport/build/report/";
  private static final String REPORT_FILE_NAME = "d3-test-report.html";

  public static void main(String[] args) throws IOException {
    MustacheFactory mf = new DefaultMustacheFactory();
    Mustache mustache = mf.compile(REPORT_TEMPLATE_FILE);
    File reportFolder = new File(REPORT_FOLDER);
    if (!reportFolder.exists() && !reportFolder.mkdirs()) {
      System.err.println("Cannot create report folder " + REPORT_FOLDER);
      System.exit(1);
    }
    try (Writer writer = new BufferedWriter(new FileWriter(REPORT_FOLDER + REPORT_FILE_NAME))) {
      mustache.execute(writer, REPORT_SERVICE.create(TestFolder.getAllFolders())).flush();
      System.out.println("Report was successfully created. Take a look at " + REPORT_FOLDER + REPORT_FILE_NAME);
    }
  }
}
