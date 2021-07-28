package de.variantsync.studies.sync.diff;

import java.util.List;

public interface IContextProvider {
    List<Line> leadingContext(ILineFilter lineFilter, FileDiff fileDiff, Hunk hunk, int lineNumber);
    List<Line> trailingContext(ILineFilter lineFilter, FileDiff fileDiff, Hunk hunk, int lineNumber);
}