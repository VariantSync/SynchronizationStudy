package de.variantsync.studies.sync.diff.splitting;

import de.variantsync.studies.sync.diff.components.FileDiff;
import de.variantsync.studies.sync.diff.filter.ILineFilter;
import de.variantsync.studies.sync.diff.lines.Line;

import java.nio.file.Path;
import java.util.List;

public class NaiveContextProvider extends DefaultContextProvider {
    public NaiveContextProvider(Path rootDir) {
        super(rootDir);
    }

    public NaiveContextProvider(Path rootDir, int contextSize) {
        super(rootDir, contextSize);
    }

    @Override
    public List<Line> leadingContext(ILineFilter lineFilter, FileDiff fileDiff, int index) {
        return super.leadingContext((p, i) -> true, fileDiff, index);
    }

    @Override
    public List<Line> trailingContext(ILineFilter lineFilter, FileDiff fileDiff, int index) {
        return super.trailingContext((p, i) -> true, fileDiff, index);
    }
}
