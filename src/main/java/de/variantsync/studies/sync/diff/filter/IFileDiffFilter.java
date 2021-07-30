package de.variantsync.studies.sync.diff.filter;

import de.variantsync.studies.sync.diff.components.FileDiff;

public interface IFileDiffFilter{
    boolean shouldKeep(FileDiff fileDiff);
}