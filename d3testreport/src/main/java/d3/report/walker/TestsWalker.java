package d3.report.walker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/*
    This class is used to collect test files in a test folder
 */
public class TestsWalker {
    private static final String TEST_FILE_EXT = ".kt";
    private static final String TEST_POSTFIX = "test";

    /**
     * Returns all test files a given folder
     *
     * @param rootFolder root folder where all the tests are stored
     * @return test files
     */
    public List<String> getTestFiles(String rootFolder) {
        List<String> testFiles = new ArrayList<>();
        collectTestFiles(rootFolder, testFiles);
        return testFiles;
    }

    private void collectTestFiles(String rootFolder, List<String> testFiles) {
        File root = new File(rootFolder);
        File[] list = root.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) {
                collectTestFiles(f.getAbsolutePath(), testFiles);
            } else {
                String filePath = f.getAbsolutePath();
                if (filePath.endsWith(TEST_FILE_EXT)
                        && f.getName().toLowerCase().contains(TEST_POSTFIX)) {
                    testFiles.add(filePath);
                }
            }
        }
    }
}
