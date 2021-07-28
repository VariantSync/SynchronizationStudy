package de.variantsync.studies.sync.diff.components;

import java.util.LinkedList;
import java.util.List;

public record FileDiff(List<String> header, List<Hunk> hunks, String oldFile, String newFile) implements IDiffComponent {
    @Override
    public List<String> toLines() {
        List<String> lines = new LinkedList<>(header);
        hunks.stream().map(IDiffComponent::toLines).forEach(lines::addAll);
        return lines;
    }
}