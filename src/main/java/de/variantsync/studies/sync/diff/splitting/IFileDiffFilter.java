package de.variantsync.studies.sync.diff.splitting;

import de.variantsync.studies.sync.diff.components.FileDiff;

public interface IFileDiffFilter{
    Boolean shouldKeep(FileDiff fileDiff);
}