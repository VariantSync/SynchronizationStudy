package de.variantsync.studies.sync.diff;

import java.util.List;

public interface IContextProvider {
    List<ContextLine> leadingContext(String filePath, int lineNumber, Line line);
    List<ContextLine> trailingContext(String filePath, int lineNumber, Line line);
}