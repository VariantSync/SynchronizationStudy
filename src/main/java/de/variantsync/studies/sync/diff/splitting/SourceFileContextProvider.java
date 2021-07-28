package de.variantsync.studies.sync.diff.splitting;

import de.variantsync.evolution.util.NotImplementedException;
import de.variantsync.studies.sync.diff.components.FileDiff;
import de.variantsync.studies.sync.diff.components.Hunk;
import de.variantsync.studies.sync.diff.lines.Line;

import java.util.List;

public class SourceFileContextProvider implements IContextProvider {
    DefaultContextProvider defaultContextProvider = new DefaultContextProvider();

    @Override
    public List<Line> leadingContext(ILineFilter lineFilter, FileDiff fileDiff, Hunk hunk, int lineNumber) {
        throw new NotImplementedException();
    }

    @Override
    public List<Line> trailingContext(ILineFilter lineFilter, FileDiff fileDiff, Hunk hunk, int lineNumber) {
        throw new NotImplementedException();
    }
}