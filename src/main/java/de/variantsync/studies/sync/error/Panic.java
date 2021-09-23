package de.variantsync.studies.sync.error;

public class Panic extends Error {
    public Panic(final String message) {
        super(message);
    }

    public Panic() {
        super();
    }
}
