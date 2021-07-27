package de.variantsync.studies.sync.diff;

import de.variantsync.evolution.util.NotImplementedException;

import java.util.ArrayList;
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
        if (lines.get(0).startsWith("diff")) {
            // Several files were processed
        } else if (lines.get(0).startsWith("---")) {
            // Only one file was processed
        }

        throw new NotImplementedException();
    }
}