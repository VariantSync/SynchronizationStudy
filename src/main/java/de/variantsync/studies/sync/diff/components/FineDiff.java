package de.variantsync.studies.sync.diff.components;

import java.util.LinkedList;
import java.util.List;

public class FineDiff implements IDiffComponent {
    private final List<FileDiff> content;

    public FineDiff(List<FileDiff> content) {
        this.content = content;
    }

    @Override
    public List<String> toLines() {
        List<String> lines = new LinkedList<>();
        content.stream().map(IDiffComponent::toLines).forEach(lines::addAll);
        return lines;
    }
}