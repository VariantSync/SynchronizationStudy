package de.variantsync.studies.sync.diff.splitting;

import de.variantsync.studies.sync.diff.components.FileDiff;

public class DefaultFileDiffFilter implements IFileDiffFilter {

    @Override
    public Boolean shouldKeep(FileDiff fileDiff) {
        return true;
    }
}