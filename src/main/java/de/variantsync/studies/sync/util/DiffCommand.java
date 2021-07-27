package de.variantsync.studies.sync.util;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;

public class DiffCommand implements IShellCommand {
    private static final String COMMAND = "diff";
    private final String[] files;
    private final LinkedList<String> args = new LinkedList<>();

    public DiffCommand(String... files) {
        this.files = files;
    }

    public static DiffCommand Recommended(Path pathA, Path pathB) {
        return new DiffCommand(pathA.toString(), pathB.toString())
                .newFile()
                .text()
                .unified()
                .recursive();
    }

    @Override
    public String[] parts() {
        String[] parts = new String[files.length + args.size() + 1];

        parts[0] = COMMAND;
        int index = 0;
        for (; index < args.size(); index++) {
            parts[index + 1] = args.get(index);
        }
        for (; index < args.size() + files.length; index++) {
            parts[index + 1] = files[index - args.size()];
        }
        return parts;
    }

    /**
     * Treat absent files as empty
     *
     * @return this command
     */
    public DiffCommand newFile() {
        args.add("-N");
        return this;
    }

    /**
     * Treat all files as text
     *
     * @return this command
     */
    public DiffCommand text() {
        args.add("-a");
        return this;
    }

    /**
     * Output three lines of unified context
     *
     * @return this command
     */
    public DiffCommand unified() {
        args.add("-u");
        return this;
    }

    /**
     * Recursively compare any subdirectories found
     *
     * @return this command
     */
    public DiffCommand recursive() {
        args.add("-r");
        return this;
    }

    @Override
    public String toString() {
        return "diff: " + Arrays.toString(parts());
    }
}