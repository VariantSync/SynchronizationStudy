package de.variantsync.studies.sync.error;

public class ShellException extends Exception {

    public ShellException(Exception e) {
        super(e);
    }

    public ShellException(String s) {
        super(s);
    }
}