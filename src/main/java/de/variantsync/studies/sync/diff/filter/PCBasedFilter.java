package de.variantsync.studies.sync.diff.filter;

import de.variantsync.evolution.util.CaseSensitivePath;
import de.variantsync.evolution.util.Logger;
import de.variantsync.evolution.variability.config.IConfiguration;
import de.variantsync.evolution.variability.pc.Artefact;
import de.variantsync.studies.sync.diff.components.FileDiff;
import de.variantsync.studies.sync.error.Panic;
import org.prop4j.Node;

import java.nio.file.Path;

public class PCBasedFilter implements IFileDiffFilter, ILineFilter{
    private final Artefact oldTraces;
    private final Artefact newTraces;
    private final IConfiguration oldConfig;
    private final IConfiguration newConfig;
    private final Path oldVersion;
    private final Path newVersion;
    
    PCBasedFilter(IConfiguration oldConfig, IConfiguration newConfig, Artefact oldTraces, Artefact newTraces, Path oldVersionRoot, Path newVersionRoot) {
        this.oldConfig = oldConfig;
        this.newConfig = newConfig;
        this.oldTraces = oldTraces;
        this.newTraces = newTraces;
        oldVersion = oldVersionRoot;
        newVersion = newVersionRoot;
    }

    @Override
    public boolean shouldKeep(FileDiff fileDiff) {
        // Files are filtered implicitly, if all lines are filtered
        return true;
    }

    @Override
    public boolean shouldKeep(Path filePath, int index) {
        if (filePath.startsWith(oldVersion)) {
            return shouldKeep(oldConfig, oldTraces, filePath, index);
        } else if (filePath.startsWith(newVersion)) {
            return shouldKeep(newConfig, newTraces, filePath, index);
        } else {
            String message = "The given path '" + filePath + "' does not match any of the versions' paths";
            Logger.error(message);
            throw new Panic(message);
        }
    }
    
    private static boolean shouldKeep(IConfiguration configuration, Artefact traces, Path filePath, int index) {
        Node pc = traces
                .getPresenceConditionOf(new CaseSensitivePath(filePath), index)
                .expect("Was not able to load PC for line " + index + " of " + filePath);
        return configuration.satisfies(pc);
    }
}
