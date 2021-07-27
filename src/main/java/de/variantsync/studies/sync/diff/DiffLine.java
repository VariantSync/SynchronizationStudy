package de.variantsync.studies.sync.diff;

public abstract class DiffLine {
    private final String line;

    protected DiffLine(String line) {
        this.line = line;
    }

    public String getLine() {
        return line;
    }
}