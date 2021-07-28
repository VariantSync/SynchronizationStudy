package de.variantsync.studies.sync.diff;

import java.util.LinkedList;
import java.util.List;

public record Hunk(HunkLocation sourceLocation, HunkLocation targetLocation, List<Line> content) implements IDiffComponent {

    @Override
    public List<String> toLines() {
        List<String> lines = new LinkedList<>();
        lines.add(String.format("@@ -%d,%d +%d,%d @@", sourceLocation.startLine(), sourceLocation.size(), targetLocation.startLine(), targetLocation.size()));
        content.stream().map(Line::line).forEach(lines::add);
        return lines;
    }
}