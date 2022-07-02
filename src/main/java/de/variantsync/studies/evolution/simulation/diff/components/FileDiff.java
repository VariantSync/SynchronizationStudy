package de.variantsync.studies.evolution.simulation.diff.components;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public record FileDiff(List<String> header, List<Hunk> hunks, Path oldFile, Path newFile) implements IDiffComponent {
    @Override
    public List<String> toLines() {
        final List<String> lines = new LinkedList<>(header);
        hunks.stream().map(IDiffComponent::toLines).forEach(lines::addAll);
        return lines;
    }
}