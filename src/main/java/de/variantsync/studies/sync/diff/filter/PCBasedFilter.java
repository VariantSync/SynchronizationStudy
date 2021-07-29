package de.variantsync.studies.sync.diff.filter;

import de.variantsync.evolution.util.NotImplementedException;
import de.variantsync.studies.sync.diff.components.FileDiff;

import java.nio.file.Path;

// TODO: Implement next
public class PCBasedFilter implements IFileDiffFilter, ILineFilter{
    @Override
    public Boolean shouldKeep(FileDiff fileDiff) {
        throw new NotImplementedException();
    }

    @Override
    public Boolean shouldKeep(Path filePath, int index) {
        throw new NotImplementedException();
    }
}
