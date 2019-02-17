package d3.report.service;

import d3.report.enums.TestFolder;
import d3.report.model.Report;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestReportServiceTest {

    private TestReportService testReportService = new TestReportService();

    /**
     * @given test folders full of integration test code
     * @when create() method is called
     * @then created report is not empty
     */
    @Test
    public void testReportIsNotEmpty() throws IOException {
        for (String testFolder : TestFolder.getAllFolders()) {
            Report report = testReportService.create(Collections.singletonList("../" + testFolder));
            assertFalse(report.getReportData().isEmpty());
        }
    }
}
