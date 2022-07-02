package de.variantsync.studies.evolution.simulation.diff.lines;

import java.nio.file.Path;

public record Change(Path file, Line line) {
}
