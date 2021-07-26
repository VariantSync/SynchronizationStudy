package de.variantsync.studies.sync.util;

public interface IShellCommand {
    /***
     * Return the String parts that define and configure the command execution (e.g., ["echo", "Hello World"])
     *
     * @return the parts of the shell command.
     */
    String[] commandParts();
}