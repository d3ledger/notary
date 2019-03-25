/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import d3.report.enums.TestFolder;
import d3.report.service.TestReportService;

import java.io.IOException;

/*
    Main class to read report in plain JSON
 */
public class MainJson {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final TestReportService REPORT_SERVICE = new TestReportService();

    public static void main(String[] args) throws IOException {
        System.out.println(GSON.toJson(REPORT_SERVICE.create(TestFolder.getAllFolders())));
    }
}
