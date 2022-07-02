package de.variantsync.studies.evolution.simulation.diff.filter;

import de.variantsync.studies.evolution.simulation.diff.components.FileDiff;

public class DefaultFileDiffFilter implements IFileDiffFilter {

    @Override
    public boolean shouldKeep(final FileDiff fileDiff) {
        return true;
    }
}