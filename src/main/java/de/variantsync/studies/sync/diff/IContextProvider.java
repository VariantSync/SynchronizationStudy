package de.variantsync.studies.sync.diff;

import java.util.List;

public interface IContextProvider {
    List<Line> leadingContext(String filePath, Hunk hunk, int lineNumber);
    List<Line> trailingContext(String filePath, Hunk hunk, int lineNumber);
}