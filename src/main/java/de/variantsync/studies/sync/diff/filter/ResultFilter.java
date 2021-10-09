package de.variantsync.studies.sync.diff.filter;

import de.variantsync.evolution.feature.Variant;
import de.variantsync.evolution.util.Logger;
import de.variantsync.evolution.variability.pc.Artefact;
import de.variantsync.studies.sync.error.Panic;

import java.nio.file.Path;

public class ResultFilter extends EditFilter {
    public ResultFilter(Artefact oldTraces, Artefact newTraces, Variant targetVariant, Path oldVersionRoot, Path newVersionRoot, int strip) {
        super(oldTraces, newTraces, targetVariant, oldVersionRoot, newVersionRoot, strip);
    }

    @Override
    public boolean keepEdit(final Path filePath, final int index) {
        // When filtering results so that only differences are kept that exist due to incorrect patching, we automatically
        // keep all LineRemoval edits. We return true for all lines that are checked to be in the old version, because
        // removed lines are supposed to be in the old version, while added files (that still have to be checked) are only
        // in the newVersion
        if (oldVersion.endsWith(filePath.getName(0))) {
            return true;
        } else if (newVersion.endsWith(filePath.getName(0))) {
            return shouldKeep(targetVariant, newTraces, filePath, index);
        } else {
            final String message = "The given path '" + filePath + "' does not match any of the versions' paths";
            Logger.error(message);
            throw new Panic(message);
        }
    }
}
