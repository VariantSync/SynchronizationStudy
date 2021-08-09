import de.variantsync.evolution.Main;
import de.variantsync.studies.sync.diff.DiffParser;
import de.variantsync.studies.sync.diff.components.FineDiff;
import de.variantsync.studies.sync.diff.filter.ILineFilter;
import de.variantsync.studies.sync.diff.splitting.DiffSplitter;
import de.variantsync.studies.sync.diff.splitting.IContextProvider;
import de.variantsync.studies.sync.diff.splitting.PerfectContextProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class PerfectContextProviderTest {
    Path resourceDir = Paths.get("src", "test", "resources", "context", "splits");
    
    static {
        Main.Initialize();
    }

    private void runComparison(Path pathToExpectedResult, ILineFilter lineFilter, Path targetDir) throws IOException {
        List<String> diffLines = Files.readAllLines(resourceDir.getParent().resolve("diff-A-B.txt"));
        IContextProvider contextProvider = new PerfectContextProvider(resourceDir.getParent(), targetDir, 3, 1);
        FineDiff fineDiff = DiffSplitter.split(DiffParser.toOriginalDiff(diffLines), null, lineFilter, contextProvider);

        List<String> expectedLines = Files.readAllLines(pathToExpectedResult);
        List<String> actualLines = fineDiff.toLines();
        Assertions.assertEquals(expectedLines, actualLines);
    }

    @Test
    public void basicValidation() throws IOException {
        Path targetDir = resourceDir.getParent().resolve("version-A");
        runComparison(resourceDir.getParent().resolve("fine-diff-A-B.txt"), null, targetDir);
    }

    /**
     * Start of test regarding leading context
     */

    @Test
    public void firstLeadingContextLineCut() throws IOException {
        Path targetDir = resourceDir.getParent().resolve("target").resolve("firstLeadingContextLineCut");
        Path pathToExpectedResult = resourceDir.resolve("firstLeadingContextLineCut.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-B") && i == 6);
        runComparison(pathToExpectedResult, lineFilter, targetDir);
    }

    @Test
    public void secondLeadingContextLineCut() throws IOException {
        Path targetDir = resourceDir.getParent().resolve("target").resolve("secondLeadingContextLineCut");
        Path pathToExpectedResult = resourceDir.resolve("secondLeadingContextLineCut.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-B") && i == 7);
        runComparison(pathToExpectedResult, lineFilter, targetDir);
    }

    @Test
    public void AllLeadingContextLineCut() throws IOException {
        Path targetDir = resourceDir.getParent().resolve("target").resolve("AllLeadingContextLineCut");
        Path pathToExpectedResult = resourceDir.resolve("AllLeadingContextLineCut.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-B") && i >= 6 && i < 9);
        runComparison(pathToExpectedResult, lineFilter, targetDir);
    }

    @Test
    public void leadingContextCutAndFirstLineInSourceContext() throws IOException {
        Path targetDir = resourceDir.getParent().resolve("target").resolve("leadingContextCutAndFirstLineInSourceContext");
        Path pathToExpectedResult = resourceDir.resolve("leadingContextCutAndFirstLineInSourceContext.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-B") && (i >= 5 && i < 9));
        runComparison(pathToExpectedResult, lineFilter, targetDir);
    }

    @Test
    public void leadingContextCutAndSecondLineInSourceContext() throws IOException {
        Path targetDir = resourceDir.getParent().resolve("target").resolve("leadingContextCutAndSecondLineInSourceContext");
        Path pathToExpectedResult = resourceDir.resolve("leadingContextCutAndSecondLineInSourceContext.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-B") && (i >= 46 && i < 49 || i == 44));
        runComparison(pathToExpectedResult, lineFilter, targetDir);
    }

    @Test
    public void leadingContextCutAndAllSourceContext() throws IOException {
        Path targetDir = resourceDir.getParent().resolve("target").resolve("leadingContextCutAndAllSourceContext");
        Path pathToExpectedResult = resourceDir.resolve("leadingContextCutAndAllSourceContext.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-B") && i < 49);
        runComparison(pathToExpectedResult, lineFilter, targetDir);
    }

    @Test
    public void leadingContextCutAndStartOfSource() throws IOException {
        Path targetDir = resourceDir.getParent().resolve("target").resolve("leadingContextCutAndStartOfSource");
        Path pathToExpectedResult = resourceDir.resolve("leadingContextCutAndStartOfSource.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-B") && i < 9);
        runComparison(pathToExpectedResult, lineFilter, targetDir);
    }

    /***
     * Start of tests regarding trailing context
     */

    @Test
    public void firstTrailingContextLineCut() throws IOException {
        Path targetDir = resourceDir.getParent().resolve("target").resolve("firstTrailingContextLineCut");
        Path pathToExpectedResult = resourceDir.resolve("firstTrailingContextLineCut.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-A") && i==9);
        runComparison(pathToExpectedResult, lineFilter, targetDir);
    }

    @Test
    public void secondTrailingContextLineCut() throws IOException {
        Path targetDir = resourceDir.getParent().resolve("target").resolve("secondTrailingContextLineCut");
        Path pathToExpectedResult = resourceDir.resolve("secondTrailingContextLineCut.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-A") && i==50);
        runComparison(pathToExpectedResult, lineFilter, targetDir);
    }

    @Test
    public void AllTrailingContextLineCut() throws IOException {
        Path targetDir = resourceDir.getParent().resolve("target").resolve("AllTrailingContextLineCut");
        Path pathToExpectedResult = resourceDir.resolve("AllTrailingContextLineCut.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-A") && i>=114 && i < 117);
        runComparison(pathToExpectedResult, lineFilter, targetDir);
    }

    @Test
    public void trailingContextCutAndFirstLineInSourceContext() throws IOException {
        Path targetDir = resourceDir.getParent().resolve("target").resolve("trailingContextCutAndFirstLineInSourceContext");
        Path pathToExpectedResult = resourceDir.resolve("trailingContextCutAndFirstLineInSourceContext.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-A") && i>=114 && i < 118);
        runComparison(pathToExpectedResult, lineFilter, targetDir);
    }

    @Test
    public void trailingContextCutAndSecondLineInSourceContext() throws IOException {
        Path targetDir = resourceDir.getParent().resolve("target").resolve("trailingContextCutAndSecondLineInSourceContext");
        Path pathToExpectedResult = resourceDir.resolve("trailingContextCutAndSecondLineInSourceContext.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-A") && (i>=9 && i < 12 || i == 13));
        runComparison(pathToExpectedResult, lineFilter, targetDir);
    }

    @Test
    public void trailingContextCutAndLinesInSourceContextEOFNext() throws IOException {
        Path targetDir = resourceDir.getParent().resolve("target").resolve("trailingContextCutAndLinesInSourceContextEOFNext");
        Path pathToExpectedResult = resourceDir.resolve("trailingContextCutAndLinesInSourceContextEOFNext.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-A") && i>=114 && i < 122);
        runComparison(pathToExpectedResult, lineFilter, targetDir);
    }

    @Test
    public void trailingContextCutAndAllSourceContext() throws IOException {
        Path targetDir = resourceDir.getParent().resolve("target").resolve("trailingContextCutAndAllSourceContext");
        Path pathToExpectedResult = resourceDir.resolve("trailingContextCutAndAllSourceContext.txt");
        ILineFilter lineFilter = (f, i)
                -> !(f.toString().contains("version-A") && (i>=9 && i < 12 || i == 14));
        runComparison(pathToExpectedResult, lineFilter, targetDir);
    }

    @Test
    public void trailingContextCutAndEndOfSource() throws IOException {
        Path targetDir = resourceDir.getParent().resolve("target").resolve("trailingContextCutAndEndOfSource");
        Path pathToExpectedResult = resourceDir.resolve("trailingContextCutAndEndOfSource.txt");
        ILineFilter lineFilter = (f, i)
                -> !(i>=10);
        runComparison(pathToExpectedResult, lineFilter, targetDir);
    }
}
