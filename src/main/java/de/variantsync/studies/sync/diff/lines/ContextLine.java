package de.variantsync.studies.sync.diff.lines;

public class ContextLine extends Line {
    public ContextLine(final String line) {
        super(line);
    }

    public ContextLine(final AddedLine line) {
        super(line.line().replaceFirst("\\+", " "));
    }

    public ContextLine(final RemovedLine removedLine) {
        super(removedLine.line().replaceFirst("-", " "));
    }
}