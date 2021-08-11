package de.variantsync.studies.sync.diff.filter;

import de.variantsync.evolution.feature.Variant;
import de.variantsync.evolution.variability.pc.Artefact;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EditFilter extends PCBasedFilter {
    private final Map<EditLocation, Boolean> keepDecisions = new HashMap<>();
    
    public EditFilter(Artefact oldTraces, Artefact newTraces, Variant targetVariant, Path oldVersionRoot, Path newVersionRoot) {
        super(oldTraces, newTraces, targetVariant, oldVersionRoot, newVersionRoot);
    }

    public EditFilter(Artefact oldTraces, Artefact newTraces, Variant targetVariant, Path oldVersionRoot, Path newVersionRoot, int strip) {
        super(oldTraces, newTraces, targetVariant, oldVersionRoot, newVersionRoot, strip);
    }

    @Override
    public boolean keepEdit(Path filePath, int index) {
        EditLocation editLocation = new EditLocation(filePath, index);
        boolean keep = super.keepEdit(filePath, index);
        keepDecisions.put(editLocation, keep);
        return keep;
    }
    
    @Override
    public boolean keepContext(Path filePath, int index) {
        EditLocation editLocation = new EditLocation(filePath, index);
        boolean decision = keepDecisions.getOrDefault(editLocation, true);
        return decision;
    }
    
    private record EditLocation(Path filePath, int index) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EditLocation that = (EditLocation) o;
            return index == that.index && Objects.equals(filePath, that.filePath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(filePath, index);
        }
    }
}
