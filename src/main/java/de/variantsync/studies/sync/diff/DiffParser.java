package de.variantsync.studies.sync.diff;

import de.variantsync.evolution.util.NotImplementedException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DiffParser {

    public static FineDiff toFineDiff(OriginalDiff originalDiff) {
        throw new NotImplementedException();
    }

    public static OriginalDiff toOriginalDiff(List<String> lines) {
        // The diff is empty, but this is also a valid scenario
        if (lines.isEmpty()) {
            return new OriginalDiff(new ArrayList<>());
        }
        List<FileDiff> fileDiffs = new LinkedList<>();
        // Determine the substring which a FileDiff starts with
        String fileDiffStart = "";
        if (lines.get(0).startsWith("diff")) {
            // Several files were processed, the diff of each file starts with the 'diff' command that was used
            fileDiffStart = "diff";
        } else if (lines.get(0).startsWith("---")) {
            // Only one file was processed, the diff of the file starts with the hunk header
            fileDiffStart = "---";
        }

        List<String> fileDiffContent = null;
        for (String line : lines) {
            if (line.startsWith(fileDiffStart)) {
                // Create a FileDiff from the collected lines
                if (fileDiffContent != null) {
                    fileDiffs.add(parseFileDiff(fileDiffContent));
                }
                // Reset the lines that should go into the next FileDiff
                fileDiffContent = new LinkedList<>();
            }
            if (fileDiffContent == null) {
                throw new IllegalArgumentException("The provided lines do not contain one of the expected fileDiffStart values");
            }
            fileDiffContent.add(line);
        }

        return new OriginalDiff(fileDiffs);
    }

    private static FileDiff parseFileDiff(List<String> fileDiffContent) {
        throw new NotImplementedException();
    }
}