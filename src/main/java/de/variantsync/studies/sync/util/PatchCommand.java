package de.variantsync.studies.sync.util;

import de.variantsync.evolution.util.NotImplementedException;

import java.nio.file.Path;

public class PatchCommand implements IShellCommand{
    public PatchCommand(Path patchFile) {
        throw new NotImplementedException();
    }

    @Override
    public String[] commandParts() {
        throw new NotImplementedException();
    }

    public PatchCommand outfile(Path outputPath) {
        throw new NotImplementedException();
    }
}