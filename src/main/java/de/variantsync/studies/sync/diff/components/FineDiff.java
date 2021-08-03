package de.variantsync.studies.sync.diff.components;

import java.util.LinkedList;
import java.util.List;

public record FineDiff(
        List<FileDiff> content) implements IDiffComponent {

    @Override
    public List<String> toLines() {
        List<String> lines = new LinkedList<>();
        content.stream().map(IDiffComponent::toLines).forEach(lines::addAll);
        return lines;
    }
}