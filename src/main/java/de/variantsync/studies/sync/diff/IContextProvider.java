package de.variantsync.studies.sync.diff;

import de.variantsync.studies.sync.util.Pair;

import java.util.List;

public interface IContextProvider {
    Pair<List<Line>, NumIgnoredLines> leadingContext(ILineFilter lineFilter, FileDiff fileDiff, Hunk hunk, int lineNumber);
    Pair<List<Line>, NumIgnoredLines> trailingContext(ILineFilter lineFilter, FileDiff fileDiff, Hunk hunk, int lineNumber);
}