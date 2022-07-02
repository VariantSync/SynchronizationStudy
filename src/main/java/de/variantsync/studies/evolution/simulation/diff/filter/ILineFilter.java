package de.variantsync.studies.evolution.simulation.diff.filter;

import java.nio.file.Path;

public interface ILineFilter {
    boolean keepEdit(Path filePath, int index);

    default boolean keepContext(final Path filePath, final int index) {
        return keepEdit(filePath, index);
    }
}