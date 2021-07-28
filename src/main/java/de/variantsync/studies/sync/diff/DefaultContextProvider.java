package de.variantsync.studies.sync.diff;

import de.variantsync.evolution.util.NotImplementedException;

import java.util.List;

public class DefaultContextProvider implements IContextProvider {
    private final OriginalDiff originalDiff;

    public DefaultContextProvider(OriginalDiff originalDiff) {
        this.originalDiff = originalDiff;
    }

    @Override
    public List<ContextLine> leadingContext(String filePath, int lineNumber, Line line) {
        throw new NotImplementedException();
    }

    @Override
    public List<ContextLine> trailingContext(String filePath, int lineNumber, Line line) {
        throw new NotImplementedException();
    }
}