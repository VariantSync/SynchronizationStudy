package de.variantsync.studies.sync.diff.filter;

import java.nio.file.Path;

public interface ILineFilter{
    Boolean shouldKeep(Path filePath, int index);
}