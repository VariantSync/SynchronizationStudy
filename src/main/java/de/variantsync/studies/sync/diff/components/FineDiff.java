package de.variantsync.studies.sync.diff.components;

import de.variantsync.studies.sync.diff.lines.AddedLine;
import de.variantsync.studies.sync.diff.lines.Line;
import de.variantsync.studies.sync.diff.lines.RemovedLine;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public record FineDiff(
        List<FileDiff> content) implements IDiffComponent {

    @Override
    public List<String> toLines() {
        final List<String> lines = new LinkedList<>();
        content.stream().map(IDiffComponent::toLines).forEach(lines::addAll);
        return lines;
    }

    public static List<Line> determineChangedLines(FineDiff diff) {
        final List<Line> changedLines = new LinkedList<>();
        diff.content().stream().flatMap(fd -> fd.hunks().stream()).flatMap(hunk -> hunk.content().stream()).forEach(line -> {
                    if (line instanceof AddedLine addedLine) {
                        changedLines.add(addedLine);
                    } else if (line instanceof RemovedLine removedLine) {
                        changedLines.add(removedLine);
                    }
                }
        );
        return changedLines;
    }

    public static List<AddedLine> calculateAddedLines(FineDiff diff) {
        return determineChangedLines(diff).stream().filter(l -> l instanceof AddedLine).map(l -> (AddedLine) l).collect(Collectors.toList());
    }

    public static List<RemovedLine> calculateRemovedLines(FineDiff diff) {
        return determineChangedLines(diff).stream().filter(l -> l instanceof RemovedLine).map(l -> (RemovedLine) l).collect(Collectors.toList());

    }
}