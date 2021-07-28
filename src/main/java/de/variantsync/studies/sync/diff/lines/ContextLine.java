package de.variantsync.studies.sync.diff.lines;

public class ContextLine extends Line {
    public ContextLine(String line) {
        super(line);
    }

    public ContextLine(AddedLine line) {
        super(line.line().replaceFirst("\\+", " "));
    }

    public ContextLine(RemovedLine removedLine) {
        super(removedLine.line().replaceFirst("-", " "));
    }
}