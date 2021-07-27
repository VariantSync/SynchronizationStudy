package de.variantsync.studies.sync.util;

import de.variantsync.evolution.util.Logger;
import de.variantsync.evolution.util.functional.Result;
import de.variantsync.evolution.util.functional.Unit;
import de.variantsync.studies.sync.error.SetupError;
import de.variantsync.studies.sync.error.ShellException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ShellExecutor {
    private final Consumer<String> outputReader;
    private final Consumer<String> errorReader;
    private final Path workDir;

    public ShellExecutor(Consumer<String> outputReader, Consumer<String> errorReader) {
        this.workDir = null;
        this.outputReader = outputReader;
        this.errorReader = errorReader;
    }

    public ShellExecutor(Consumer<String> outputReader, Consumer<String> errorReader, Path workDir) {
        this.workDir = workDir;
        this.outputReader = outputReader;
        this.errorReader = errorReader;
    }

    public Result<Unit, ShellException> execute(IShellCommand command) {
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            throw new SetupError("The synchronization study can only be executed under Linux!");
        }

        ProcessBuilder builder = new ProcessBuilder();
        if (workDir != null) {
            builder.directory(workDir.toFile());
        }
        Logger.debug("Executing '" + command + "' in directory " + builder.directory());
        builder.command(command.parts());

        Process process;
        try {
            process = builder.start();
            Executors.newSingleThreadExecutor()
                    .submit(collectOutput(process.getInputStream(), outputReader));
            Executors.newSingleThreadExecutor()
                    .submit(collectOutput(process.getErrorStream(), outputReader));
        } catch (IOException e) {
            Logger.error("Was not able to execute " + command, e);
            return Result.Failure(new ShellException(e));
        }

        return Result.Success(Unit.Instance());
    }

    private Runnable collectOutput(InputStream inputStream, Consumer<String> consumer) {
        return () -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                reader.lines().forEach(consumer);
            } catch (IOException e) {
                Logger.error("Exception thrown while reading stream of Shell command.", e);
            }
        };
    }
}