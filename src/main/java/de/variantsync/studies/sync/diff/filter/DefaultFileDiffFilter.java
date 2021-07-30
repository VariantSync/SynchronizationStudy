package de.variantsync.studies.sync.diff.filter;

import de.variantsync.studies.sync.diff.components.FileDiff;

public class DefaultFileDiffFilter implements IFileDiffFilter {

    @Override
    public boolean shouldKeep(FileDiff fileDiff) {
        return true;
    }
}