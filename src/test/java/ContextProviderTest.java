import de.variantsync.evolution.util.NotImplementedException;
import de.variantsync.studies.sync.diff.DiffParser;
import de.variantsync.studies.sync.diff.components.FineDiff;
import de.variantsync.studies.sync.diff.splitting.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ContextProviderTest {
    Path resourceDir = Paths.get("src", "test", "resources", "context", "splits");

    private void runComparison(Path pathToExpectedResult, ILineFilter lineFilter) throws IOException {
        List<String> diffLines = Files.readAllLines(resourceDir.getParent().resolve("diff-A-B.txt"));
        IContextProvider contextProvider = new DefaultContextProvider(resourceDir.getParent());
        FineDiff fineDiff = DiffSplitter.split(DiffParser.toOriginalDiff(diffLines), null, lineFilter, contextProvider);

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
        runComparison(resourceDir.getParent().resolve("fine-diff-A-B.txt"), null);
    }

    /**
     * Start of test regarding leading context
     */

    @Test
    public void firstLeadingContextLineCut() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("firstLeadingContextLineCut.txt");
        ILineFilter lineFilter = (f, i) 
                -> !(f.toString().contains("version-B") && i == 6);
        runComparison(pathToExpectedResult, lineFilter);
    }

    @Test
    public void secondLeadingContextLineCut() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("secondLeadingContextLineCut.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-B") && i == 7);
        runComparison(pathToExpectedResult, lineFilter);
    }

    @Test
    public void AllLeadingContextLineCut() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("AllLeadingContextLineCut.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-B") && i >= 6 && i < 9);
        runComparison(pathToExpectedResult, lineFilter);
    }

    @Test
    public void leadingContextCutAndFirstLineInSourceContext() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("AllLeadingContextLineCut.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-B") && (i == 6 || i == 5));
        runComparison(pathToExpectedResult, lineFilter);
    }

    @Test
    public void leadingContextCutAndSecondLineInSourceContext() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("leadingContextCutAndSecondLineInSourceContext.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-B") && (i >= 45 && i < 48 || i == 43));
        runComparison(pathToExpectedResult, lineFilter);
    }

    @Test
    public void leadingContextCutAndAllSourceContext() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("leadingContextCutAndAllSourceContext.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-B") && i < 48);
        runComparison(pathToExpectedResult, lineFilter);
    }

    @Test
    public void leadingContextCutAndStartOfSource() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("leadingContextCutAndStartOfSource.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-B") && i < 8);
        runComparison(pathToExpectedResult, lineFilter);
    }

    /***
     * Start of tests regarding trailing context
     */

    @Test
    public void firstTrailingContextLineCut() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("firstTrailingContextLineCut.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-A") && i==9);
        runComparison(pathToExpectedResult, lineFilter);
    }

    @Test
    public void secondTrailingContextLineCut() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("secondTrailingContextLineCut.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-A") && i==50);
        runComparison(pathToExpectedResult, lineFilter);
    }

    @Test
    public void AllTrailingContextLineCut() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("AllTrailingContextLineCut.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-A") && i>=114 && i < 117);
        runComparison(pathToExpectedResult, lineFilter);
    }

    @Test
    public void trailingContextCutAndFirstLineInSourceContext() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("trailingContextCutAndFirstLineInSourceContext.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-A") && i>=114 && i < 118);
        runComparison(pathToExpectedResult, lineFilter);
    }

    @Test
    public void trailingContextCutAndSecondLineInSourceContext() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("trailingContextCutAndSecondLineInSourceContext.txt");
        ILineFilter lineFilter = (f, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, lineFilter);
    }

    @Test
    public void trailingContextCutAndOneLineInSourceContextEOFNext() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("trailingContextCutAndOneLineInSourceContextEOFNext.txt");
        ILineFilter lineFilter = (f, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, lineFilter);
    }

    @Test
    public void trailingContextCutAndAllSourceContext() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("trailingContextCutAndAllSourceContext.txt");
        ILineFilter lineFilter = (f, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, lineFilter);
    }

    @Test
    public void trailingContextCutAndEndOfSource() throws IOException {
        Path pathToExpectedResult = resourceDir.resolve("trailingContextCutAndEndOfSource.txt");
        ILineFilter lineFilter = (f, i) -> {
            throw new NotImplementedException();
        };
        runComparison(pathToExpectedResult, lineFilter);
    }
}