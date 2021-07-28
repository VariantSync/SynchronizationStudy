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
    public void parseBackToLines() throws IOException {
        Path resourceDir = Paths.get("src", "test", "resources", "patch-breakdown");
        List<String> diffLines = Files.readAllLines(Paths.get(resourceDir.toString(), "diff-A-B.txt"));
        IDiffComponent originalDiff = DiffParser.toOriginalDiff(diffLines);
        Assertions.assertEquals(diffLines, originalDiff.toLines());
    }

    @Test
    public void loadOriginalDiff() throws IOException {
        Path resourceDir = Paths.get("src", "test", "resources", "patch-breakdown");
        List<String> diffLines = Files.readAllLines(Paths.get(resourceDir.toString(), "diff-A-B.txt"));
        OriginalDiff originalDiff = DiffParser.toOriginalDiff(diffLines);

        List<FileDiff> fileDiffs = originalDiff.fileDiffs();

        assert fileDiffs.size() == 3;

        for (int i = 0; i < fileDiffs.size(); i++) {
            FileDiff fileDiff = fileDiffs.get(i);
            assert fileDiff.hunks().size() == 1;
            Hunk hunk = fileDiff.hunks().get(0);
            HunkLocation location = hunk.location();
            List<Line> hunkContent = hunk.content();

            switch (i) {
                case 0 -> {
                    assert fileDiff.oldFile().equals("version-A/first-file.txt");
                    assert fileDiff.newFile().equals("version-B/first-file.txt");
                    assert location.startLineTarget() == 9;
                    assert hunkContent.size() == 9;
                    assert hunkContent.stream().filter(l -> l instanceof ContextLine).count() == 6;
                    assert hunkContent.stream().filter(l -> l instanceof AddedLine).count() == 2;
                    assert hunkContent.stream().noneMatch(l -> l instanceof RemovedLine);
                }
                case 1 -> {
                    assert fileDiff.oldFile().equals("version-A/second-file.txt");
                    assert fileDiff.newFile().equals("version-B/second-file.txt");
                    assert location.startLineSource() == 9;
                    assert location.startLineTarget() == 9;
                    assert hunkContent.size() == 16;
                    assert hunkContent.stream().filter(l -> l instanceof ContextLine).count() == 11;
                    assert hunkContent.stream().filter(l -> l instanceof AddedLine).count() == 2;
                    assert hunkContent.stream().filter(l -> l instanceof RemovedLine).count() == 2;
                }
                case 2 -> {
                    assert fileDiff.oldFile().equals("version-A/third-file.txt");
                    assert fileDiff.newFile().equals("version-B/third-file.txt");
                    assert location.startLineSource() == 1;
                    assert location.startLineTarget() == 1;
                    assert hunkContent.size() == 12;
                    assert hunkContent.stream().filter(l -> l instanceof ContextLine).count() == 4;
                    assert hunkContent.stream().noneMatch(l -> l instanceof AddedLine);
                    assert hunkContent.stream().filter(l -> l instanceof RemovedLine).count() == 7;
                }
                default -> throw new AssertionError("Unexpected hunk.");
            }
        }
    }
}