package de.variantsync.studies.sync.util;

import java.util.Arrays;

public class EchoCommand implements IShellCommand {
    private final String COMMAND = "echo";
    private final String message;

    public EchoCommand(String echoMessage) {
        this.message = echoMessage;
    }

    @Override
    public String[] parts() {
        String[] parts = new String[2];
        parts[0] = COMMAND;
        parts[1] = message;
        return parts;
    }

    @Override
    public String toString() {
        return "echo: " + Arrays.toString(parts());
    }
}