package de.variantsync.studies.sync.diff;

import java.util.LinkedList;
import java.util.List;

public class FineDiff implements IDiffComponent {
    private final List<FileDiff> content;

    // Package private as it should not be possible to create a FineDiff manually
    FineDiff(List<FileDiff> content) {
        this.content = content;
    }

    @Override
    public List<String> toLines() {
        List<String> lines = new LinkedList<>();
        content.stream().map(IDiffComponent::toLines).forEach(lines::addAll);
        return lines;
    }
}