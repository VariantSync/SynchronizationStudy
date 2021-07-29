package de.variantsync.studies.sync.diff.splitting;

import java.nio.file.Path;

public class DefaultLineFilter implements ILineFilter {
    @Override
    public Boolean shouldKeep(Path filePath, int index) {
        return true;
    }
}