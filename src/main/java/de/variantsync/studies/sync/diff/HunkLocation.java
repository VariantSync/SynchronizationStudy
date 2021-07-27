package de.variantsync.studies.sync.diff;

public record HunkLocation(String relativePath, int startLine, int size) {
}