package de.variantsync.studies.sync.diff;

import java.util.function.Function;

public interface IFileDiffFilter extends Function<FileDiff, Boolean> {
    Boolean apply(FileDiff fileDiff);
}