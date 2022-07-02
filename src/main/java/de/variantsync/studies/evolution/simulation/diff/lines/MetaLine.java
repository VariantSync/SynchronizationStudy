package de.variantsync.studies.evolution.simulation.diff.lines;

public class MetaLine extends Line {
    public MetaLine(final String line) {
        super(line);
    }

    public MetaLine() {
        super("\\ No newline at end of file");
    }
}