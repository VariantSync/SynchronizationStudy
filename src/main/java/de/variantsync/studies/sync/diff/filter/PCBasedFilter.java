package de.variantsync.studies.sync.diff.filter;

import de.variantsync.evolution.io.ResourceLoader;
import de.variantsync.evolution.io.kernelhaven.KernelHavenVariantPCIO;
import de.variantsync.evolution.util.CaseSensitivePath;
import de.variantsync.evolution.util.NotImplementedException;
import de.variantsync.evolution.variability.pc.Artefact;
import de.variantsync.studies.sync.diff.components.FileDiff;

import java.nio.file.Path;

// TODO: Implement next
public class PCBasedFilter implements IFileDiffFilter, ILineFilter{
    private final Artefact oldTraces;
    private final Artefact newTraces;
    
    PCBasedFilter(Path oldPCs, Path newPCs) {
        ResourceLoader<Artefact> variantPCIO = new KernelHavenVariantPCIO();
        oldTraces = variantPCIO.load(oldPCs).expect("Was not able to load PCs: " + oldPCs);
        newTraces = variantPCIO.load(newPCs).expect("Was not able to Load PCs: " + newPCs);
    }

    @Override
    public Boolean shouldKeep(FileDiff fileDiff) {
        throw new NotImplementedException();
    }

    @Override
    public Boolean shouldKeep(Path filePath, int index) {
        throw new NotImplementedException();
    }
    
    private static boolean getPresenceCondition(Artefact traces, Path filePath, int index) {
//        traces.
//        Node result = traces.getPresenceConditionOf(new CaseSensitivePath(filePath), index).expect("Was not able to load PC for line " + index + " of " + filePath);
        throw new NotImplementedException();
    }
}
