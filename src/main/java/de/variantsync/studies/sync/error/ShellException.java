package de.variantsync.studies.sync.error;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ShellException extends Exception {
    private final List<String> output; 

    public ShellException(final Exception e) {
        super(e);
        this.output = new LinkedList<>();
    }

    public ShellException(final List<String> output) {
        super(convert(output));
        this.output = output;
    }

    public List<String> getOutput() {
        return output;
    }

    private static String convert(final Collection<String> output) {
        final StringBuilder sb = new StringBuilder();
        output.forEach(l -> sb.append(l).append(System.lineSeparator()));
        return sb.toString();
    }
}