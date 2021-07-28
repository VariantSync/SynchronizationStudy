package de.variantsync.studies.sync.diff;

import de.variantsync.evolution.util.NotImplementedException;

import java.util.List;

public record FineDiff(List<FileDiff> content) implements IDiffComponent {
    @Override
    public List<String> toLines() {
        throw new NotImplementedException();
    }
}