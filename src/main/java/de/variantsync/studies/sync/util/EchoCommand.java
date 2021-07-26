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
        args.addFirst(COMMAND);
        args.addLast(message);
        return args.toArray(new String[0]);
    }

    @Override
    public String toString() {
        return "echo: " + Arrays.toString(parts());
    }
}