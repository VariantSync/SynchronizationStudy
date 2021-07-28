package de.variantsync.studies.sync.diff.components;

import de.variantsync.studies.sync.diff.lines.AddedLine;
import de.variantsync.studies.sync.diff.lines.Line;
import de.variantsync.studies.sync.diff.lines.MetaLine;
import de.variantsync.studies.sync.diff.lines.RemovedLine;

import java.util.LinkedList;
import java.util.List;

public record Hunk(HunkLocation location, List<Line> content) implements IDiffComponent {

    @Override
    public List<String> toLines() {
        List<String> lines = new LinkedList<>();
        int sourceSize = (int) content.stream().filter(l -> !(l instanceof AddedLine || l instanceof MetaLine)).count();
        int targetSize = (int) content.stream().filter(l -> !(l instanceof RemovedLine || l instanceof MetaLine)).count();
        lines.add(String.format("@@ -%d,%d +%d,%d @@", location.startLineSource(), sourceSize, location.startLineTarget(), targetSize));
        content.stream().map(Line::line).forEach(lines::add);
        return lines;
    }
}