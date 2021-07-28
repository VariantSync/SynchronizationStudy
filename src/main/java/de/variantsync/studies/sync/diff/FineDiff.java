package de.variantsync.studies.sync.diff;

import de.variantsync.evolution.util.NotImplementedException;

import java.util.List;

public class FineDiff implements IDiffComponent {
    private final List<FileDiff> content;

    // Package private as it should not be possible to create a FineDiff manually
    FineDiff(List<FileDiff> content) {
        this.content = content;
    }

    @Override
    public List<String> toLines() {
        throw new NotImplementedException();
    }
}