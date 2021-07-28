package de.variantsync.studies.sync.diff;

public class FilteredLine extends Line {
    protected FilteredLine(String line) {
        super(line);
    }

    protected FilteredLine(Line other) {
        super(other.line());
    }
}