package de.variantsync.studies.sync.diff.lines;

import java.nio.file.Path;

public record Change(Path file, Line line) {
}
