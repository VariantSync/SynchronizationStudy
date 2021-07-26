package de.variantsync.studies.sync.util;

import java.util.Arrays;
import java.util.LinkedList;

public class EchoCommand implements IShellCommand {
    private final LinkedList<String> args = new LinkedList<>();
    private final String COMMAND = "echo";
    private final String message;

    public EchoCommand(String echoMessage) {
        this.message = echoMessage;
    }

    @Override
    public String[] parts() {
        String[] parts = new String[args.size() + 2];
        parts[0] = COMMAND;
        int index = 0;
        for (; index < args.size(); index++) {
            parts[index+1] = args.get(index);
        }
        parts[index+1] = message;
        return parts;
    }

    @Override
    public String toString() {
        return "echo: " + Arrays.toString(parts());
    }
}