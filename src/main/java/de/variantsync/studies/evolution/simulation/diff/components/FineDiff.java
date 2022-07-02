package de.variantsync.studies.evolution.simulation.diff.components;

import de.variantsync.studies.evolution.simulation.diff.lines.AddedLine;
import de.variantsync.studies.evolution.simulation.diff.lines.Change;
import de.variantsync.studies.evolution.simulation.diff.lines.RemovedLine;

import java.util.LinkedList;
import java.util.List;

public record FineDiff(List<FileDiff> content) implements IDiffComponent {

    public static List<Change> determineChangedLines(FineDiff diff) {
        final List<Change> changedLines = new LinkedList<>();
        for (FileDiff fd : diff.content()) {
            fd.hunks().stream().flatMap(hunk -> hunk.content().stream()).forEach(line -> {
                        if (line instanceof AddedLine addedLine) {
                            changedLines.add(new Change(fd.oldFile().subpath(2, fd.oldFile().getNameCount()), addedLine));
                        } else if (line instanceof RemovedLine removedLine) {
                            changedLines.add(new Change(fd.oldFile().subpath(2, fd.oldFile().getNameCount()), removedLine));
                        }
                    }
            );
        }
        return changedLines;
    }

    @Override
    public List<String> toLines() {
        final List<String> lines = new LinkedList<>();
        content.stream().map(IDiffComponent::toLines).forEach(lines::addAll);
        return lines;
    }
}