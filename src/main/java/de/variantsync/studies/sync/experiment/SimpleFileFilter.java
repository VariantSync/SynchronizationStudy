package de.variantsync.studies.sync.experiment;

import de.variantsync.evolution.variability.pc.SourceCodeFile;
import de.variantsync.evolution.variability.pc.options.ArtefactFilter;

import java.nio.file.Path;
import java.util.Set;

public record SimpleFileFilter(
        Set<Path> filesToKeep) implements ArtefactFilter<SourceCodeFile> {

    @Override
    public boolean shouldKeep(final SourceCodeFile sourceCodeFile) {
        return filesToKeep.contains(sourceCodeFile.getFile().path());
    }
}
