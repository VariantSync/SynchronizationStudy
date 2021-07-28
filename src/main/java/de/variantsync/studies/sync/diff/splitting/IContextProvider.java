package de.variantsync.studies.sync.diff.splitting;

import de.variantsync.studies.sync.diff.components.FileDiff;
import de.variantsync.studies.sync.diff.components.Hunk;
import de.variantsync.studies.sync.diff.lines.Line;

import java.util.List;

public interface IContextProvider {
    List<Line> leadingContext(ILineFilter lineFilter, FileDiff fileDiff, Hunk hunk, int lineNumber);
    List<Line> trailingContext(ILineFilter lineFilter, FileDiff fileDiff, Hunk hunk, int lineNumber);
}