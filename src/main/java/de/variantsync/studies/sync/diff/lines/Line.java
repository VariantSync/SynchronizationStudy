package de.variantsync.studies.sync.diff.lines;

public abstract class Line {
    private final String line;

    protected Line(final String line) {
        this.line = line;
    }

    public String line() {
        return line;
    }

    @Override
    public String toString() {
        return this.line;
    }
}