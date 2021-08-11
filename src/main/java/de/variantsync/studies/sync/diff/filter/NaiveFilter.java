package de.variantsync.studies.sync.diff.filter;

import de.variantsync.evolution.feature.Variant;
import de.variantsync.evolution.variability.pc.Artefact;

import java.nio.file.Path;

public class NaiveFilter extends PCBasedFilter {

    public NaiveFilter(Artefact oldTraces, Artefact newTraces, Variant targetVariant, Path oldVersionRoot, Path newVersionRoot) {
        super(oldTraces, newTraces, targetVariant, oldVersionRoot, newVersionRoot);
    }

    public NaiveFilter(Artefact oldTraces, Artefact newTraces, Variant targetVariant, Path oldVersionRoot, Path newVersionRoot, int strip) {
        super(oldTraces, newTraces, targetVariant, oldVersionRoot, newVersionRoot, strip);
    }

    @Override
    public boolean keepContext(Path filePath, int index) {
        return true;
    }

}
