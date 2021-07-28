import de.variantsync.studies.sync.diff.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DiffSplitterTest {
    Path resourceDir = Paths.get("src", "test", "resources", "patch-breakdown", "splits");

    private void runComparison(Path pathToExpectedResult, IContextProvider contextProvider, IFileDiffFilter fileDiffFilter, ILineFilter lineFilter) throws IOException {
        List<String> diffLines = Files.readAllLines(resourceDir.getParent().resolve("diff-A-B.txt"));
        FineDiff fineDiff;
        if (contextProvider == null && fileDiffFilter == null && lineFilter == null) {
            fineDiff = DiffSplitter.split(DiffParser.toOriginalDiff(diffLines));
        } else if (contextProvider != null && fileDiffFilter != null && lineFilter != null) {
            fineDiff = DiffSplitter.split(DiffParser.toOriginalDiff(diffLines), fileDiffFilter, lineFilter, contextProvider);
        } else {
            throw new IllegalArgumentException();
        }

        List<String> expectedLines = Files.readAllLines(pathToExpectedResult);
        List<String> actualLines = fineDiff.toLines();
        Assertions.assertEquals(actualLines.size(), expectedLines.size());
        for (int i = 0; i < expectedLines.size(); i++) {
            String expectedLine = expectedLines.get(i);
            String actualLine = actualLines.get(i);
            Assertions.assertEquals(expectedLine, actualLine);
        }
    }

    private void runComparison(Path pathToExpectedResult) throws IOException {
        runComparison(pathToExpectedResult, null, null, null);
    }

    @Test
    public void splitToBasicFineDiff() throws IOException {
        Path pathToExpectedResult = resourceDir.getParent().resolve("fine-diff-A-B.txt");
        runComparison(pathToExpectedResult);
    }

    @Test
    public void filterAllFiles() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("filterAllFiles.txt");
        runComparison(pathToExpectedResult);
    }

    @Test
    public void filterFirstFile() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("filterFirstFile.txt");
        runComparison(pathToExpectedResult);
    }

    @Test
    public void filterEmptyLineOfThirdFile() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("filterEmptyLineOfThirdFile.txt");
        runComparison(pathToExpectedResult);
    }

    @Test
    public void filterAllHunksOfSecondFile() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("filterAllHunksOfSecondFile.txt");
        runComparison(pathToExpectedResult);
    }

    @Test
    public void filterMyObjEditsInThirdFile() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("filterMyObjEditsInThirdFile.txt");
        runComparison(pathToExpectedResult);
    }

    @Test
    public void filterZumZumInsertionInFirstFile() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("filterZumZumInsertionInFirstFile.txt");
        runComparison(pathToExpectedResult);
    }

    @Test
    public void filterBlablaInsertionInFirstFile() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("filterBlablaInsertionInFirstFile.txt");
        runComparison(pathToExpectedResult);
    }

    @Test
    public void filterFordDeletionInSecondFile() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("filterFordDeletionInSecondFile.txt");
        runComparison(pathToExpectedResult);
    }

    @Test
    public void filterMazdaDeletionInSecondFile() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("filterMazdaDeletionInSecondFile.txt");
        runComparison(pathToExpectedResult);
    }

    @Test
    public void filterOneLineOfLeadingContextOfFirstFile() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("filterOneLineOfLeadingContextOfFirstFile.txt");
        runComparison(pathToExpectedResult);
    }

    @Test
    public void filterTwoLinesOfLeadingContextOfFirstFile() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("filterTwoLinesOfLeadingContextOfFirstFile.txt");
        runComparison(pathToExpectedResult);
    }

    @Test
    public void filterAllLinesOfLeadingContextOfFirstFile() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("filterAllLinesOfLeadingContextOfFirstFile.txt");
        runComparison(pathToExpectedResult);
    }

    @Test
    public void filterOneLineOfContextBetweenEditsOfSecondFile() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("filterOneLineOfContextBetweenEditsOfSecondFile.txt");
        runComparison(pathToExpectedResult);
    }

    @Test
    public void filterThreeLinesOfContextBetweenEditsOfSecondFile() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("filterThreeLinesOfContextBetweenEditsOfSecondFile.txt");
        runComparison(pathToExpectedResult);
    }

    @Test
    public void filterAllContextBetweenEditsOfSecondFile() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("filterAllContextBetweenEditsOfSecondFile.txt");
        runComparison(pathToExpectedResult);
    }

    @Test
    public void filterTrailingContextOfThirdFile() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("filterTrailingContextOfThirdFile.txt");
        runComparison(pathToExpectedResult);
    }
}