package de.variantsync.studies.sync.diff.filter;

import de.variantsync.evolution.feature.Variant;
import de.variantsync.evolution.util.io.CaseSensitivePath;
import de.variantsync.evolution.util.Logger;
import de.variantsync.evolution.util.functional.Result;
import de.variantsync.evolution.variability.pc.Artefact;
import de.variantsync.studies.sync.diff.components.FileDiff;
import de.variantsync.studies.sync.error.Panic;
import org.prop4j.Node;

import java.nio.file.Path;

public class PCBasedFilter implements IFileDiffFilter, ILineFilter {
    private final Artefact oldTraces;
    private final Artefact newTraces;
    private final Variant targetVariant;
    private final Path oldVersion;
    private final Path newVersion;
    private final int strip;

    public PCBasedFilter(final Artefact oldTraces, final Artefact newTraces, final Variant targetVariant, final Path oldVersionRoot, final Path newVersionRoot) {
        this.oldTraces = oldTraces;
        this.newTraces = newTraces;
        this.targetVariant = targetVariant;
        this.oldVersion = oldVersionRoot;
        this.newVersion = newVersionRoot;
        this.strip = 0;
    }

    public PCBasedFilter(final Artefact oldTraces, final Artefact newTraces, final Variant targetVariant, final Path oldVersionRoot, final Path newVersionRoot, final int strip) {
        this.oldTraces = oldTraces;
        this.newTraces = newTraces;
        this.targetVariant = targetVariant;
        this.oldVersion = oldVersionRoot;
        this.newVersion = newVersionRoot;
        this.strip = strip;
    }

    private boolean shouldKeep(final Variant targetVariant, final Artefact traces, Path filePath, final int index) {
        filePath = filePath.subpath(strip, filePath.getNameCount());
        final Node pc = traces
                .getPresenceConditionOf(new CaseSensitivePath(filePath), index)
                .expect("Was not able to load PC for line " + index + " of " + filePath);
        return targetVariant.isImplementing(pc);
    }

    private boolean shouldKeep(final Variant targetVariant, final Artefact traces, Path filePath) {
        filePath = filePath.subpath(strip, filePath.getNameCount());
        final Result<Node, Exception> result = traces.getPresenceConditionOf(new CaseSensitivePath(filePath));
        if (result.isFailure()) {
            Logger.warning("No PC found for " + filePath);
            return false;
        } else {
            final Node pc = result.getSuccess();
            return targetVariant.isImplementing(pc);
        }
    }

    @Override
    public boolean shouldKeep(final FileDiff fileDiff) {
        return shouldKeep(targetVariant, oldTraces, fileDiff.oldFile()) && shouldKeep(targetVariant, newTraces, fileDiff.newFile());
    }

    @Override
    public boolean keepEdit(final Path filePath, final int index) {
        if (oldVersion.endsWith(filePath.getName(0))) {
            return shouldKeep(targetVariant, oldTraces, filePath, index);
        } else if (newVersion.endsWith(filePath.getName(0))) {
            return shouldKeep(targetVariant, newTraces, filePath, index);
        } else {
            final String message = "The given path '" + filePath + "' does not match any of the versions' paths";
            Logger.error(message);
            throw new Panic(message);
        }
    }
}
