import de.variantsync.studies.sync.diff.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DiffParserTest {

    @Test
    public void loadOriginalDiff() throws IOException {
        Path resourceDir = Paths.get("src", "test", "resources", "patch-breakdown");
        List<String> diffLines = Files.readAllLines(Paths.get(resourceDir.toString(), "diff-A-B.txt"));
        OriginalDiff originalDiff = DiffParser.toOriginalDiff(diffLines);

        List<DiffHunk> hunks = originalDiff.hunks();
        assert hunks.size() == 3;

        for (int i = 0; i < hunks.size(); i++) {
            DiffHunk hunk = hunks.get(i);
            HunkLocation sourceLocation = hunk.sourceLocation();
            HunkLocation targetLocation = hunk.targetLocation();
            List<DiffLine> hunkContent = hunk.content();

            switch (i) {
                case 0 -> {
                    assert sourceLocation.relativePath().equals("version-A/first-file.txt");
                    assert sourceLocation.startLine() == 9;
                    assert sourceLocation.size() == 6;
                    assert targetLocation.relativePath().equals("version-B/first-file.txt");
                    assert targetLocation.startLine() == 9;
                    assert targetLocation.size() == 8;
                    assert hunkContent.size() == 8;
                    assert hunkContent.stream().filter(l -> l instanceof ContextLine).count() == 6;
                    assert hunkContent.stream().filter(l -> l instanceof AddedLine).count() == 2;
                    assert hunkContent.stream().noneMatch(l -> l instanceof RemovedLine);
                }
                case 1 -> {
                    assert sourceLocation.relativePath().equals("version-A/second-file.txt");
                    assert sourceLocation.startLine() == 9;
                    assert sourceLocation.size() == 13;
                    assert targetLocation.relativePath().equals("version-B/second-file.txt");
                    assert targetLocation.startLine() == 9;
                    assert targetLocation.size() == 13;
                    assert hunkContent.size() == 15;
                    assert hunkContent.stream().filter(l -> l instanceof ContextLine).count() == 11;
                    assert hunkContent.stream().filter(l -> l instanceof AddedLine).count() == 2;
                    assert hunkContent.stream().filter(l -> l instanceof RemovedLine).count() == 2;
                }
                case 2 -> {
                    assert sourceLocation.relativePath().equals("version-A/third-file.txt");
                    assert sourceLocation.startLine() == 1;
                    assert sourceLocation.size() == 11;
                    assert targetLocation.relativePath().equals("version-B/third-file.txt");
                    assert targetLocation.startLine() == 1;
                    assert targetLocation.size() == 4;
                    assert hunkContent.size() == 11;
                    assert hunkContent.stream().filter(l -> l instanceof ContextLine).count() == 4;
                    assert hunkContent.stream().noneMatch(l -> l instanceof AddedLine);
                    assert hunkContent.stream().filter(l -> l instanceof RemovedLine).count() == 7;
                }
                default -> throw new AssertionError("Unexpected hunk.");
            }
        }
    }

    @Test
    public void exampleDiffParsedCorrectly() throws IOException {
        Path resourceDir = Paths.get("src", "test", "resources", "patch-breakdown");
        List<String> diffLines = Files.readAllLines(Paths.get(resourceDir.toString(), "diff-A-B.txt"));
        FineDiff fineDiff = DiffParser.toFineDiff(DiffParser.toOriginalDiff(diffLines));

        List<String> expectedResult = Files.readAllLines(Paths.get(resourceDir.toString(), "fine-diff-A-B.txt"));
        Assertions.assertEquals(expectedResult, fineDiff.content());
    }
}