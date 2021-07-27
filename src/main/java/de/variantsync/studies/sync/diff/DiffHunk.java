package de.variantsync.studies.sync.diff;

import de.variantsync.evolution.util.NotImplementedException;

import java.util.List;

public class DiffHunk {
    private final List<String> hunk;
    private final String fileNameBefore;
    private final int startLineBefore;
    private final int hunkSizeBefore;

    private final String fileNameAfter;
    private final int startLineAfter;
    private final int hunkSizeAfter;

    private List<String> prefixContext;
    private List<String> postfixContext;

    public DiffHunk(List<String> lines) {
        hunk = lines;

        throw new NotImplementedException();
    }
}