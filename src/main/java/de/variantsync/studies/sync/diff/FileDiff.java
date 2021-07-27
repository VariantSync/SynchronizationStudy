package de.variantsync.studies.sync.diff;

import java.util.List;

public record FileDiff(List<String> header, List<Hunk> hunks, String oldFile, String newFile) {
}