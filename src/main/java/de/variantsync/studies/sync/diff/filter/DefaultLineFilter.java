package de.variantsync.studies.sync.diff.filter;

import java.nio.file.Path;

public class DefaultLineFilter implements ILineFilter {
    @Override
    public boolean shouldKeep(Path filePath, int index) {
        return true;
    }
}