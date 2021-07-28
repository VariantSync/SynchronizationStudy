import de.variantsync.studies.sync.diff.DiffParser;
import de.variantsync.studies.sync.diff.DiffSplitter;
import de.variantsync.studies.sync.diff.FineDiff;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DiffSplitterTest {
    @Test
    public void exampleDiffParsedCorrectly() throws IOException {
        Path resourceDir = Paths.get("src", "test", "resources", "patch-breakdown");
        List<String> diffLines = Files.readAllLines(Paths.get(resourceDir.toString(), "diff-A-B.txt"));
        FineDiff fineDiff = DiffSplitter.split(DiffParser.toOriginalDiff(diffLines));

        List<String> expectedResult = Files.readAllLines(Paths.get(resourceDir.toString(), "fine-diff-A-B.txt"));
        Assertions.assertEquals(expectedResult, fineDiff.toLines());
    }
}