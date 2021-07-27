import de.variantsync.evolution.util.Logger;
import de.variantsync.evolution.util.functional.Result;
import de.variantsync.evolution.util.functional.Unit;
import de.variantsync.studies.sync.error.ShellException;
import de.variantsync.studies.sync.util.DiffCommand;
import de.variantsync.studies.sync.util.EchoCommand;
import de.variantsync.studies.sync.util.PatchCommand;
import de.variantsync.studies.sync.util.ShellExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ShellExecutorTest {
    static Consumer<String> errorReader = Logger::error;

    static {
        Logger.initConsoleLogger();
    }

    @Test
    public void echo() {
        String testString = "This is a call from ShellTest.";
        Consumer<String> outputReader =
                s -> {
                    Logger.info(s);
                    assert testString.equals(s);
                };
        Logger.info("I am here");

        ShellExecutor shellExecutor = new ShellExecutor(outputReader, errorReader);
        shellExecutor.execute(new EchoCommand(testString));
    }

    @Test
    public void simpleDiff() throws IOException {
        Path resourcesDir = Paths.get("src", "test", "resources", "simple-text");
        List<String> output = new ArrayList<>();

        Consumer<String> outputReader = output::add;
        ShellExecutor shellExecutor = new ShellExecutor(outputReader, errorReader, resourcesDir);

        Path pathA = Paths.get("text-A.txt");
        Path pathB = Paths.get("text-B.txt");

        Result<Unit, ShellException> result =
                shellExecutor.execute(DiffCommand.Recommended(pathA, pathB));

        assert result.isSuccess();
        List<String> expectedDiff =
                Files.readAllLines(Paths.get(resourcesDir.toString(), "diff-A-B.txt"));
        assert expectedDiff.size() == output.size();
        for (int i = 2; i < expectedDiff.size(); i++) {
            Assertions.assertEquals(expectedDiff.get(i), output.get(i));
        }
    }

    @Test
    public void diffToFile() throws IOException {
        Path resourcesDir = Paths.get("src", "test", "resources", "simple-text");
        Path outputPath = Files.createTempFile("patch", null);

        List<String> output = new ArrayList<>();
        Consumer<String> outputReader = output::add;

        ShellExecutor shellExecutor = new ShellExecutor(outputReader, errorReader, resourcesDir);

        Path pathA = Paths.get("text-A.txt");
        Path pathB = Paths.get("text-B.txt");

        Result<Unit, ShellException> result =
                shellExecutor.execute(DiffCommand.Recommended(pathA, pathB));
        assert result.isSuccess();
        Files.write(outputPath, output);

        // Compare the written output with the expected diff
        List<String> expectedDiff =
                Files.readAllLines(Paths.get(resourcesDir.toString(), "diff-A-B.txt"));
        List<String> actualDiff = Files.readAllLines(outputPath);

        assert expectedDiff.size() == actualDiff.size();
        for (int i = 2; i < expectedDiff.size(); i++) {
            Assertions.assertEquals(expectedDiff.get(i), actualDiff.get(i));
        }
    }

    @Test
    public void patchFile() throws IOException {
        Path resourcesDir = Paths.get("src", "test", "resources", "simple-text");
        Path outputPath = Files.createTempFile("patch-result", ".txt");

        Consumer<String> outputReader = Logger::info;
        ShellExecutor shellExecutor = new ShellExecutor(outputReader, errorReader, resourcesDir);

        Result<Unit, ShellException> result =
                shellExecutor.execute(
                        PatchCommand.Recommended(Paths.get("diff-A-B.txt")).outfile(outputPath));
        assert result.isSuccess();
        List<String> expectedPatchResult =
                Files.readAllLines(Paths.get(resourcesDir.toString(), "patch-result.txt"));
        List<String> actualResult =
                Files.readAllLines(Paths.get(resourcesDir.toString(), "text-B.txt"));
        Assertions.assertLinesMatch(expectedPatchResult, actualResult);
    }

    @Test
    public void diffDirectories() throws IOException {
        Path resourcesDir = Paths.get("src", "test", "resources", "versions");

        List<String> actualDiff = new ArrayList<>();
        Consumer<String> outputReader = actualDiff::add;
        ShellExecutor shellExecutor = new ShellExecutor(outputReader, errorReader, resourcesDir);

        Path pathA = Paths.get("version-A");
        Path pathB = Paths.get("version-B");
        Result<Unit, ShellException> result =
                shellExecutor.execute(DiffCommand.Recommended(pathA, pathB));

        assert result.isSuccess();
        // Compare the written output with the expected diff
        List<String> expectedDiff =
                Files.readAllLines(Paths.get(resourcesDir.toString(), "diff-A-B.txt"));
        assert expectedDiff.size() == actualDiff.size();
        for (int i = 2; i < expectedDiff.size(); i++) {
            if (expectedDiff.get(i).trim().startsWith("+++")
                    || expectedDiff.get(i).trim().startsWith("---")) {
                continue;
            }
            Assertions.assertEquals(expectedDiff.get(i).trim(), actualDiff.get(i).trim());
        }
    }

    @Test
    public void patchDirectory() throws IOException {
        Path resourcesDir = Paths.get("src", "test", "resources", "versions");
        Path outputPath = Files.createTempDirectory("Version-C");

        Consumer<String> outputReader = Logger::info;
        ShellExecutor shellExecutor = new ShellExecutor(outputReader, errorReader, resourcesDir);

        Result<Unit, ShellException> result =
                shellExecutor.execute(
                        PatchCommand.Recommended(Paths.get("diff-A-B.txt")).outfile(outputPath));
        assert result.isSuccess();
        List<Path> versionBPaths =
                Files.list(Paths.get(resourcesDir.toString(), "version-B")).collect(Collectors.toList());
        List<Path> versionCPaths = Files.list(outputPath).collect(Collectors.toList());

        for (Path pathB : versionBPaths) {
            for (Path pathC : versionCPaths) {
                if (pathB.toFile().getName().equals(pathC.toFile().getName())) {
                    try {
                        Assertions.assertLinesMatch(Files.readAllLines(pathB), Files.readAllLines(pathC));
                    } catch (IOException e) {
                        throw new AssertionError(e);
                    }
                }
            }
        }
    }
}