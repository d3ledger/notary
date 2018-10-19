package d3.report.model;

/*
    Brief report information
 */
public class Summary {
    //Title of report
    private String title;
    // Root directory where all the tests are located
    private String projectRootDir;
    // Date of report generation
    private String generationDate;
    // Total amount of test files
    private int filesWithTests;
    // Total amount of test cases
    private int testCases;
    // Total amount of test cases with no properly formed description
    private int lacksInDescription;
    // Total amount of disabled test cases
    private int disabledCases;

    public String getProjectRootDir() {
        return projectRootDir;
    }

    public void setProjectRootDir(String projectRootDir) {
        this.projectRootDir = projectRootDir;
    }

    public String getGenerationDate() {
        return generationDate;
    }

    public void setGenerationDate(String generationDate) {
        this.generationDate = generationDate;
    }

    public int getFilesWithTests() {
        return filesWithTests;
    }

    public void setFilesWithTests(int filesWithTests) {
        this.filesWithTests = filesWithTests;
    }

    public int getTestCases() {
        return testCases;
    }

    public void setTestCases(int testCases) {
        this.testCases = testCases;
    }

    public int getLacksInDescription() {
        return lacksInDescription;
    }

    public void setLacksInDescription(int lacksInDescription) {
        this.lacksInDescription = lacksInDescription;
    }

    public int getDisabledCases() {
        return disabledCases;
    }

    public void setDisabledCases(int disabledCases) {
        this.disabledCases = disabledCases;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
