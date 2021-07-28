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
    Path resourceDir = Paths.get("src", "test", "resources", "patch-breakdown", "context");

    private void runComparison(Path pathToExpectedResult, IFileDiffFilter fileDiffFilter, ILineFilter lineFilter) throws IOException {
        List<String> diffLines = Files.readAllLines(resourceDir.getParent().resolve("diff-A-B.txt"));
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

    /**
     * Start of test regarding leading context
     */

    @Test
    public void firstLeadingContextLineCut() {
        throw new NotImplementedException();
    }

    @Test
    public void secondLeadingContextLineCut() {
        throw new NotImplementedException();
    }

    @Test
    public void AllLeadingContextLineCut() {
        throw new NotImplementedException();
    }

    @Test
    public void leadingContextCutAndFirstLineInSourceContext() {
        throw new NotImplementedException();
    }

    @Test
    public void leadingContextCutAndSecondLineInSourceContext() {
        throw new NotImplementedException();
    }

    @Test
    public void leadingContextCutAndAllSourceContext() {
        throw new NotImplementedException();
    }

    @Test
    public void leadingContextCutAndStartOfSource() {
        throw new NotImplementedException();
    }

    /***
     * Start of tests regarding trailing context
     */

    @Test
    public void firstTrailingContextLineCut() {
        throw new NotImplementedException();
    }

    @Test
    public void secondTrailingContextLineCut() {
        throw new NotImplementedException();
    }

    @Test
    public void AllTrailingContextLineCut() {
        throw new NotImplementedException();
    }

    @Test
    public void trailingContextCutAndFirstLineInSourceContext() {
        throw new NotImplementedException();
    }

    @Test
    public void trailingContextCutAndSecondLineInSourceContext() {
        throw new NotImplementedException();
    }

    @Test
    public void trailingContextCutAndOneLineInSourceContextEOFNext() {
        throw new NotImplementedException();
    }

    @Test
    public void trailingContextCutAndAllSourceContext() {
        throw new NotImplementedException();
    }

    @Test
    public void trailingContextCutAndEndOfSource() {
        throw new NotImplementedException();
    }
}