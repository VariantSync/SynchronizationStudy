package de.variantsync.studies.sync.diff;

public class ContextLine extends Line {
    protected ContextLine(String line) {
        super(line);
    }

    protected ContextLine(AddedLine line) {
        super(line.line().replaceFirst("\\+", " "));
    }

    protected ContextLine(MetaLine line) {
        super(line.line());
    }

    public ContextLine(RemovedLine removedLine) {
        super(removedLine.line().replaceFirst("-", " "));
    }
}