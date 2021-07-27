package de.variantsync.studies.sync.diff;

import java.util.List;

public record DiffHunk(HunkLocation sourceLocation, HunkLocation targetLocation, List<DiffLine> content) {

}