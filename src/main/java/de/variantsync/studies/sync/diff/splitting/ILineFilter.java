package de.variantsync.studies.sync.diff.splitting;

import de.variantsync.studies.sync.diff.components.FileDiff;
import de.variantsync.studies.sync.diff.components.Hunk;

public interface ILineFilter{
    Boolean shouldKeep(FileDiff fileDiff, Hunk hunk, int index);
}