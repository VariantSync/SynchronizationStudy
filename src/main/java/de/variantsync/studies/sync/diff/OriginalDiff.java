package de.variantsync.studies.sync.diff;

import java.util.List;

public record OriginalDiff(List<FileDiff> fileDiffs) {
}