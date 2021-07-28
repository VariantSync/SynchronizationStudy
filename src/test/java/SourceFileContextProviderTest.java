import de.variantsync.evolution.util.NotImplementedException;
import de.variantsync.studies.sync.diff.DiffParser;
import de.variantsync.studies.sync.diff.components.FineDiff;
import de.variantsync.studies.sync.diff.splitting.DiffSplitter;
import de.variantsync.studies.sync.diff.splitting.IFileDiffFilter;
import de.variantsync.studies.sync.diff.splitting.ILineFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SourceFileContextProviderTest {
    Path resourceDir = Paths.get("src", "test", "resources", "context");

    private void runComparison(Path pathToExpectedResult, IFileDiffFilter fileDiffFilter, ILineFilter lineFilter) throws IOException {
        List<String> diffLines = Files.readAllLines(resourceDir.resolve("diff-A-B.txt"));
        FineDiff fineDiff = DiffSplitter.split(DiffParser.toOriginalDiff(diffLines), fileDiffFilter, lineFilter, null);

        List<String> expectedLines = Files.readAllLines(pathToExpectedResult);
        List<String> actualLines = fineDiff.toLines();
        Assertions.assertEquals(expectedLines.size(), actualLines.size());
        for (int i = 0; i < expectedLines.size(); i++) {
            String expectedLine = expectedLines.get(i);
            String actualLine = actualLines.get(i);
            Assertions.assertEquals(expectedLine, actualLine);
        }
    }

    @Test
    public void basicValidation() throws IOException {
        List<String> diffLines = Files.readAllLines(resourceDir.resolve("diff-A-B.txt"));
        FineDiff fineDiff = DiffSplitter.split(DiffParser.toOriginalDiff(diffLines), null, null, null);

        List<String> expectedLines = Files.readAllLines(resourceDir.resolve("fine-diff-A-B.txt"));
        List<String> actualLines = fineDiff.toLines();
        Assertions.assertEquals(expectedLines.size(), actualLines.size());
        for (int i = 0; i < expectedLines.size(); i++) {
            String expectedLine = expectedLines.get(i);
            String actualLine = actualLines.get(i);
            Assertions.assertEquals(expectedLine, actualLine);
        }
    }

    /**
     * Start of test regarding leading context
     */

    @Test
    public void firstLeadingContextLineCut() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("balbalablablabalabla.txt");
        ILineFilter lineFilter = (f, h, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, null, lineFilter);
    }

    @Test
    public void secondLeadingContextLineCut() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("balbalablablabalabla.txt");
        ILineFilter lineFilter = (f, h, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, null, lineFilter);
    }

    @Test
    public void AllLeadingContextLineCut() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("balbalablablabalabla.txt");
        ILineFilter lineFilter = (f, h, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, null, lineFilter);
    }

    @Test
    public void leadingContextCutAndFirstLineInSourceContext() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("balbalablablabalabla.txt");
        ILineFilter lineFilter = (f, h, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, null, lineFilter);
    }

    @Test
    public void leadingContextCutAndSecondLineInSourceContext() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("balbalablablabalabla.txt");
        ILineFilter lineFilter = (f, h, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, null, lineFilter);
    }

    @Test
    public void leadingContextCutAndAllSourceContext() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("balbalablablabalabla.txt");
        ILineFilter lineFilter = (f, h, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, null, lineFilter);
    }

    @Test
    public void leadingContextCutAndStartOfSource() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("balbalablablabalabla.txt");
        ILineFilter lineFilter = (f, h, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, null, lineFilter);
    }

    /***
     * Start of tests regarding trailing context
     */

    @Test
    public void firstTrailingContextLineCut() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("balbalablablabalabla.txt");
        ILineFilter lineFilter = (f, h, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, null, lineFilter);
    }

    @Test
    public void secondTrailingContextLineCut() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("balbalablablabalabla.txt");
        ILineFilter lineFilter = (f, h, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, null, lineFilter);
    }

    @Test
    public void AllTrailingContextLineCut() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("balbalablablabalabla.txt");
        ILineFilter lineFilter = (f, h, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, null, lineFilter);
    }

    @Test
    public void trailingContextCutAndFirstLineInSourceContext() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("balbalablablabalabla.txt");
        ILineFilter lineFilter = (f, h, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, null, lineFilter);
    }

    @Test
    public void trailingContextCutAndSecondLineInSourceContext() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("balbalablablabalabla.txt");
        ILineFilter lineFilter = (f, h, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, null, lineFilter);
    }

    @Test
    public void trailingContextCutAndOneLineInSourceContextEOFNext() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("balbalablablabalabla.txt");
        ILineFilter lineFilter = (f, h, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, null, lineFilter);
    }

    @Test
    public void trailingContextCutAndAllSourceContext() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("balbalablablabalabla.txt");
        ILineFilter lineFilter = (f, h, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, null, lineFilter);
    }

    @Test
    public void trailingContextCutAndEndOfSource() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("balbalablablabalabla.txt");
        ILineFilter lineFilter = (f, h, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, null, lineFilter);
    }
}