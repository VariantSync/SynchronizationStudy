package de.variantsync.studies.sync.diff.splitting;

import java.nio.file.Path;

public interface ILineFilter{
    Boolean shouldKeep(Path filePath, int index);
}