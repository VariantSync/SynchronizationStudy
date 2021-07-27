package de.variantsync.studies.sync.diff;

import java.util.List;

public record Hunk(HunkLocation sourceLocation, HunkLocation targetLocation, List<Line> content) {

}