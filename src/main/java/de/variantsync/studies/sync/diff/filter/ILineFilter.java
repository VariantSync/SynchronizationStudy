package de.variantsync.studies.sync.diff.filter;

import java.nio.file.Path;

public interface ILineFilter {
    boolean keepEdit(Path filePath, int index);

    default boolean keepContext(Path filePath, int index) {
        return keepEdit(filePath, index);
    }
}