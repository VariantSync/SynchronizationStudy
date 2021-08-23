package de.variantsync.studies.sync.shell;

import de.variantsync.evolution.util.Logger;
import de.variantsync.evolution.util.functional.Result;
import de.variantsync.studies.sync.error.SetupError;
import de.variantsync.studies.sync.error.ShellException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    public Result<List<String>, ShellException> execute(ShellCommand command) {
        return execute(command, this.workDir);
    }

    public Result<List<String>, ShellException> execute(ShellCommand command, Path executionDir) {
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            throw new SetupError("The synchronization study can only be executed under Linux!");
        }

        ProcessBuilder builder = new ProcessBuilder();
        if (executionDir != null) {
            builder.directory(executionDir.toFile());
        }
        Logger.debug("Executing '" + command + "' in directory " + builder.directory());
        builder.command(command.parts());

        Process process;
        Future<?> outputFuture;
        Future<?> errorFuture;
        List<String> output = new LinkedList<>();
        Consumer<String> shareOutput = s -> {
            output.add(s);
            outputReader.accept(s);
        };
        try {
            process = builder.start();
            outputFuture = Executors.newSingleThreadExecutor()
                    .submit(collectOutput(process.getInputStream(), shareOutput));
            errorFuture = Executors.newSingleThreadExecutor()
                    .submit(collectOutput(process.getErrorStream(), errorReader));
        } catch (IOException e) {
            Logger.error("Was not able to execute " + command, e);
            return Result.Failure(new ShellException(e));
        }

        int exitCode;
        try {
            exitCode = process.waitFor();
            outputFuture.get();
            errorFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            Logger.error("Interrupted while waiting for process to end.", e);
            return Result.Failure(new ShellException(e));
        }
        return command.interpretResult(exitCode, output);
    }

    private Runnable collectOutput(InputStream inputStream, Consumer<String> consumer) {
        return () -> {
            try (inputStream; BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                reader.lines().forEach(consumer);
            } catch (IOException e) {
                Logger.error("Exception thrown while reading stream of Shell command.", e);
            }
        };
    }
}