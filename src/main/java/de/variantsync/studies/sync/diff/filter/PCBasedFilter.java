package de.variantsync.studies.sync.diff.filter;

import de.variantsync.evolution.feature.Variant;
import de.variantsync.evolution.util.CaseSensitivePath;
import de.variantsync.evolution.util.Logger;
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

    public PCBasedFilter(Artefact oldTraces, Artefact newTraces, Variant targetVariant, Path oldVersionRoot, Path newVersionRoot) {
        this.oldTraces = oldTraces;
        this.newTraces = newTraces;
        this.targetVariant = targetVariant;
        this.oldVersion = oldVersionRoot;
        this.newVersion = newVersionRoot;
        this.strip = 0;
    }

    public PCBasedFilter(Artefact oldTraces, Artefact newTraces, Variant targetVariant, Path oldVersionRoot, Path newVersionRoot, int strip) {
        this.oldTraces = oldTraces;
        this.newTraces = newTraces;
        this.targetVariant = targetVariant;
        this.oldVersion = oldVersionRoot;
        this.newVersion = newVersionRoot;
        this.strip = strip;
    }

    private boolean shouldKeep(Variant targetVariant, Artefact traces, Path filePath, int index) {
        filePath = filePath.subpath(strip, filePath.getNameCount());
        Node pc = traces
                .getPresenceConditionOf(new CaseSensitivePath(filePath), index)
                .expect("Was not able to load PC for line " + index + " of " + filePath);
        return targetVariant.isImplementing(pc);
    }

    private boolean shouldKeep(Variant targetVariant, Artefact traces, Path filePath) {
        filePath = filePath.subpath(strip, filePath.getNameCount());
        Node pc = traces
                .getPresenceConditionOf(new CaseSensitivePath(filePath))
                .expect("Was not able to load PC for " + filePath);
        return targetVariant.isImplementing(pc);
    }

    @Override
    public boolean shouldKeep(FileDiff fileDiff) {
        return shouldKeep(targetVariant, oldTraces, fileDiff.oldFile()) && shouldKeep(targetVariant, newTraces, fileDiff.newFile());
    }

    @Override
    public boolean shouldKeep(Path filePath, int index) {
        if (oldVersion.endsWith(filePath.getName(0))) {
            return shouldKeep(targetVariant, oldTraces, filePath, index);
        } else if (newVersion.endsWith(filePath.getName(0))) {
            return shouldKeep(targetVariant, newTraces, filePath, index);
        } else {
            String message = "The given path '" + filePath + "' does not match any of the versions' paths";
            Logger.error(message);
            throw new Panic(message);
        }
    }
}
