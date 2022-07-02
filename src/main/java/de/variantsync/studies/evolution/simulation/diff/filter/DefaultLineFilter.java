package de.variantsync.studies.evolution.simulation.diff.filter;

import java.nio.file.Path;

public class DefaultLineFilter implements ILineFilter {
    @Override
    public boolean keepEdit(final Path filePath, final int index) {
        return true;
    }
}