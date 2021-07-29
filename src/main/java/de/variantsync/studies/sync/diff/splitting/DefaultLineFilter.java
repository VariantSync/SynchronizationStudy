package de.variantsync.studies.sync.diff.splitting;

import de.variantsync.studies.sync.diff.components.Hunk;

public class DefaultLineFilter implements ILineFilter {
    @Override
    public Boolean shouldKeep(String filePath, Hunk hunk, int index) {
        return true;
    }
}