package de.variantsync.studies.sync.diff;

import de.variantsync.evolution.util.NotImplementedException;

import java.util.LinkedList;
import java.util.List;

public class DiffSplitter {

    public static FineDiff split(OriginalDiff diff) {
        return split(diff, f -> true, l -> true, new DefaultContextProvider(diff));
    }

    public static FineDiff split(OriginalDiff originalDiff, IFileDiffFilter fileFilter, ILineFilter lineFilter, IContextProvider contextProvider) {
        // The list in which we will collect the
        List<FileDiff> splitFileDiffs = new LinkedList<>();

        // Go over all FileDiff in diff
        for (FileDiff fileDiff : originalDiff.fileDiffs()) {
            // Only process file diffs that should be taken into account according to the filter
            if (fileFilter.apply(fileDiff)) {
                // Split FileDiff into edit-sized FileDiffs
                splitFileDiffs.addAll(split(fileDiff, contextProvider, lineFilter));
            }
        }

        return new FineDiff(splitFileDiffs);
    }

    private static List<FileDiff> split(FileDiff fileDiff, IContextProvider contextProvider, ILineFilter lineFilter) {
        throw new NotImplementedException();
    }
}