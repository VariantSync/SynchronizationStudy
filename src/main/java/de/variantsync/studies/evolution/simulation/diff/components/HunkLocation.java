package de.variantsync.studies.evolution.simulation.diff.components;

import java.util.Objects;

public record HunkLocation(int startLineSource, int startLineTarget) {

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final HunkLocation that = (HunkLocation) o;
        return startLineSource == that.startLineSource && startLineTarget == that.startLineTarget;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startLineSource, startLineTarget);
    }
}