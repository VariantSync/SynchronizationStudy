package de.variantsync.studies.sync.error;

import java.util.Collection;
import java.util.List;

public class ShellException extends Exception {

    public ShellException(Exception e) {
        super(e);
    }

    public ShellException(String s) {
        super(s);
    }

    public ShellException(List<String> output) {
        super(convert(output));
    }

    private static String convert(Collection<String> output) {
        StringBuilder sb = new StringBuilder();
        output.forEach(l -> sb.append(l).append(System.lineSeparator()));
        return sb.toString();
    }
}