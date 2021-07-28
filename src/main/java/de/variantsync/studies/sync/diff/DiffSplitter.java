package de.variantsync.studies.sync.diff;

import de.variantsync.evolution.util.NotImplementedException;

public class DiffSplitter {

    public static FineDiff split(OriginalDiff diff) {
        return split(diff, new DefaultContextProvider(diff), f -> true, l -> true);
    }

    public static FineDiff split(OriginalDiff diff, IContextProvider contextProvider, IFileDiffFilter fileFilter, ILineFilter lineFilter) {
        throw new NotImplementedException();
    }
}