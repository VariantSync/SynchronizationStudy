import de.variantsync.studies.sync.diff.DiffParser;
import de.variantsync.studies.sync.diff.FineDiff;
import de.variantsync.studies.sync.diff.OriginalDiff;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DiffParserTest {

    @Test
    public void exampleDiffParsedCorrectly() throws IOException {
        Path resourceDir = Paths.get("src", "test", "resources", "patch-breakdown");
        List<String> originalDiff = Files.readAllLines(Paths.get(resourceDir.toString(), "diff-A-B.txt"));
        FineDiff fineDiff = DiffParser.toFineDiff(new OriginalDiff(originalDiff));

        List<String> expectedResult = Files.readAllLines(Paths.get(resourceDir.toString(), "fine-diff-A-B.txt"));
        Assertions.assertEquals(expectedResult, fineDiff.content());
    }
}