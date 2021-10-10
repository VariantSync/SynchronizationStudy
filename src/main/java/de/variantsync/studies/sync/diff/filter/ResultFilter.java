package de.variantsync.studies.sync.diff.filter;

import de.variantsync.evolution.feature.Variant;
import de.variantsync.evolution.variability.pc.Artefact;
import de.variantsync.studies.sync.diff.components.FineDiff;

import java.nio.file.Path;

public class ResultFilter extends EditFilter {

    public ResultFilter(FineDiff expectedChangesInTarget, Artefact oldTraces, Artefact newTraces, Variant targetVariant, Path oldVersionRoot, Path newVersionRoot, int strip) {
        super(oldTraces, newTraces, targetVariant, oldVersionRoot, newVersionRoot, strip);
    }
}
