package de.variantsync.studies.sync.diff.filter;

import java.nio.file.Path;

public interface ILineFilter {
    boolean shouldKeep(Path filePath, int index);
}