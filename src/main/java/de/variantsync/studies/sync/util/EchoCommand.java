package de.variantsync.studies.sync.util;

import de.variantsync.evolution.util.NotImplementedException;

import java.util.Arrays;

public class EchoCommand implements IShellCommand {
    public EchoCommand(String echoMessage) {
        throw new NotImplementedException();

    }

    @Override
    public String[] commandParts() {
        throw new NotImplementedException();

    }

    @Override
    public String toString() {
        return "echo: " + Arrays.toString(commandParts());
    }
}