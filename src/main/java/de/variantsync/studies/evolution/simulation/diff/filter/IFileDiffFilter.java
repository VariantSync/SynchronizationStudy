package de.variantsync.studies.evolution.simulation.diff.filter;

import de.variantsync.studies.evolution.simulation.diff.components.FileDiff;

public interface IFileDiffFilter {
    boolean shouldKeep(FileDiff fileDiff);
}