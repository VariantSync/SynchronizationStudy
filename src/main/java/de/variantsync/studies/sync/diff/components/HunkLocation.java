package de.variantsync.studies.sync.diff.components;

import java.util.Objects;

public record HunkLocation(int startLineSource, int startLineTarget) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HunkLocation that = (HunkLocation) o;
        return startLineSource == that.startLineSource && startLineTarget == that.startLineTarget;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startLineSource, startLineTarget);
    }
}