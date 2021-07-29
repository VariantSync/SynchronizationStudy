package de.variantsync.studies.sync.diff.splitting;

import de.variantsync.studies.sync.diff.components.Hunk;

public interface ILineFilter{
    Boolean shouldKeep(String filePath, Hunk hunk, int index);
}