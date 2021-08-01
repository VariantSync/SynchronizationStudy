package de.variantsync.studies.sync.shell;

import de.variantsync.evolution.util.functional.Result;
import de.variantsync.studies.sync.error.ShellException;

import java.util.List;

public interface IShellCommand {
    /***
     * Return the String parts that define and configure the command execution (e.g., ["echo", "Hello World"])
     *
     * @return the parts of the shell command.
     */
    String[] parts();

    /**
     * Interpret the result code returned from a shell command
     *
     * @param resultCode the code that is to be parsed
     * @return the result
     */
    default Result<List<String>, ShellException> interpretResult(int resultCode, List<String> output) {
        return resultCode == 0 ? Result.Success(output) : Result.Failure(new ShellException(String.valueOf(resultCode)));
    }
}