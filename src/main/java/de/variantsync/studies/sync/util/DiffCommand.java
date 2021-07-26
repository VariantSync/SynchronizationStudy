package de.variantsync.studies.sync.util;

import de.variantsync.evolution.util.NotImplementedException;

import java.nio.file.Path;
import java.util.Arrays;

public class DiffCommand implements IShellCommand {
    public static DiffCommand Recommended(Path pathA, Path pathB) {
        return Recommended(pathA, pathB, null);
    }

    public static DiffCommand Recommended(Path pathA, Path pathB, Path outputPath) {
        throw new NotImplementedException();
    }

    @Override
    public String[] commandParts() {
        throw new NotImplementedException();

    }


    @Override
    public String toString() {
        return "diff: " + Arrays.toString(commandParts());
    }
}