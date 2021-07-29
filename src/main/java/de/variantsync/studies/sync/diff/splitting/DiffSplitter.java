package de.variantsync.studies.sync.diff.splitting;

import de.variantsync.studies.sync.diff.components.*;
import de.variantsync.studies.sync.diff.lines.AddedLine;
import de.variantsync.studies.sync.diff.lines.Line;
import de.variantsync.studies.sync.diff.lines.RemovedLine;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class DiffSplitter {

    public static FineDiff split(OriginalDiff diff) {
        return split(diff, null, null, null);
    }

    public static FineDiff split(OriginalDiff originalDiff, IFileDiffFilter fileFilter, ILineFilter lineFilter, IContextProvider contextProvider) {
        fileFilter = fileFilter == null ? new DefaultFileDiffFilter() : fileFilter;
        lineFilter = lineFilter == null ? new DefaultLineFilter() : lineFilter;
        contextProvider = contextProvider == null ? new DefaultContextProvider() : contextProvider;

        // The list in which we will collect the
        List<FileDiff> splitFileDiffs = new LinkedList<>();

        // Go over all FileDiff in diff
        for (FileDiff fileDiff : originalDiff.fileDiffs()) {
            // Only process file diffs that should be taken into account according to the filter
            if (fileFilter.shouldKeep(fileDiff)) {
                // Split FileDiff into edit-sized FileDiffs
                splitFileDiffs.addAll(split(fileDiff, contextProvider, lineFilter));
            }
        }

        return new FineDiff(splitFileDiffs);
    }

    private static List<FileDiff> split(FileDiff fileDiff, IContextProvider contextProvider, ILineFilter lineFilter) {
        List<FileDiff> fileDiffs = new LinkedList<>();

        for (Hunk hunk : fileDiff.hunks()) {
            // Index that points to the location of the current line in the current hunk
            int index = 0;
            for (Line line : hunk.content()) {
                if (lineFilter.shouldKeep(fileDiff, hunk, index)) {
                    if (line instanceof AddedLine || line instanceof RemovedLine) {
                        List<Line> leadingContext = contextProvider.leadingContext(lineFilter, fileDiff, hunk, index);
                        List<Line> trailingContext = contextProvider.trailingContext(lineFilter, fileDiff, hunk, index);
                        List<Line> content = new LinkedList<>(leadingContext);
                        content.add(line);
                        content.addAll(trailingContext);
                        
                        HunkLocation location = new HunkLocation(hunk.location().startLineSource(), hunk.location().startLineTarget());

                        Hunk miniHunk = new Hunk(location, content);
                        fileDiffs.add(new FileDiff(fileDiff.header(), Collections.singletonList(miniHunk), fileDiff.oldFile(), fileDiff.newFile()));
                    }
                } 
                // Increase the index
                index++;
            }
        }
        return fileDiffs;
    }
}