package de.variantsync.studies.sync.diff.splitting;

import de.variantsync.studies.sync.diff.components.FileDiff;
import de.variantsync.studies.sync.diff.filter.ILineFilter;
import de.variantsync.studies.sync.diff.lines.Line;

import java.util.List;

public interface IContextProvider {
    List<Line> leadingContext(ILineFilter lineFilter, FileDiff fileDiff, int index);
    List<Line> trailingContext(ILineFilter lineFilter, FileDiff fileDiff, int index);
}