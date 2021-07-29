package de.variantsync.studies.sync.diff.splitting;

import de.variantsync.studies.sync.diff.components.*;
import de.variantsync.studies.sync.diff.filter.DefaultFileDiffFilter;
import de.variantsync.studies.sync.diff.filter.DefaultLineFilter;
import de.variantsync.studies.sync.diff.filter.IFileDiffFilter;
import de.variantsync.studies.sync.diff.filter.ILineFilter;
import de.variantsync.studies.sync.diff.lines.AddedLine;
import de.variantsync.studies.sync.diff.lines.ContextLine;
import de.variantsync.studies.sync.diff.lines.Line;
import de.variantsync.studies.sync.diff.lines.RemovedLine;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class DiffSplitter {

    public static FineDiff split(OriginalDiff diff, IContextProvider contextProvider) {
        return split(diff, null, null, contextProvider);
    }

    public static FineDiff split(OriginalDiff originalDiff, IFileDiffFilter fileFilter, ILineFilter lineFilter, IContextProvider contextProvider) {
        fileFilter = fileFilter == null ? new DefaultFileDiffFilter() : fileFilter;
        lineFilter = lineFilter == null ? new DefaultLineFilter() : lineFilter;

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
            int sourceIndex = 0;
            int targetIndex = 0;
            for (Line line : hunk.content()) {
                if (line instanceof RemovedLine) {
                    if (lineFilter.shouldKeep(fileDiff.sourceFile(), hunk.location().startLineSource() + sourceIndex)) {
                        int leadContextStart = hunk.location().startLineTarget() + targetIndex - 1;
                        int trailContextStart = hunk.location().startLineSource() + sourceIndex + 1;
                        fileDiffs.add(calculateMiniDiff(contextProvider, lineFilter, fileDiff, hunk, line, trailContextStart, leadContextStart));
                    }
                    sourceIndex++;
                } else if (line instanceof AddedLine) {
                    if (lineFilter.shouldKeep(fileDiff.targetFile(), hunk.location().startLineTarget() + targetIndex)) {
                        int leadContextStart = hunk.location().startLineTarget() + targetIndex - 1;
                        int trailContextStart = hunk.location().startLineSource() + sourceIndex;
                        fileDiffs.add(calculateMiniDiff(contextProvider, lineFilter, fileDiff, hunk, line, trailContextStart, leadContextStart));
                    }
                    targetIndex++;
                } else if (line instanceof ContextLine) {
                    // Increase the index
                    sourceIndex++;
                    targetIndex++;
                }
            }
        }
        return fileDiffs;
    }

    private static FileDiff calculateMiniDiff(IContextProvider contextProvider, ILineFilter lineFilter,
                                              FileDiff fileDiff, Hunk hunk, Line line, int trailContextStart, int leadContextStart) {
        List<Line> leadingContext = contextProvider.leadingContext(lineFilter, fileDiff, leadContextStart);
        List<Line> trailingContext = contextProvider.trailingContext(lineFilter, fileDiff, trailContextStart);
        List<Line> content = new LinkedList<>(leadingContext);
        content.add(line);
        content.addAll(trailingContext);

        HunkLocation location = new HunkLocation(hunk.location().startLineSource(), hunk.location().startLineTarget());

        Hunk miniHunk = new Hunk(location, content);
        return new FileDiff(fileDiff.header(), Collections.singletonList(miniHunk), fileDiff.sourceFile(), fileDiff.targetFile());

    }
}