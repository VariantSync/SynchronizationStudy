package de.variantsync.studies.sync.diff;

public interface ILineFilter{
    Boolean shouldKeep(FileDiff fileDiff, Hunk hunk, int index);
}