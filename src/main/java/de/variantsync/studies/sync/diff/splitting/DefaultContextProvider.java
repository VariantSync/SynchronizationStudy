package de.variantsync.studies.sync.diff.splitting;

import de.variantsync.evolution.util.Logger;
import de.variantsync.studies.sync.diff.components.FileDiff;
import de.variantsync.studies.sync.diff.filter.ILineFilter;
import de.variantsync.studies.sync.diff.lines.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class DefaultContextProvider implements IContextProvider {
    private final int contextSize;
    private final Path rootDir;

    public DefaultContextProvider(Path rootDir) {
        // Three is the default size set in unix diff
        this(rootDir, 3);
    }

    public DefaultContextProvider(Path rootDir, int contextSize) {
        this.rootDir = rootDir;
        this.contextSize = contextSize;
    }

    @Override
    public List<Line> leadingContext(ILineFilter lineFilter, FileDiff fileDiff, int index) {
        LinkedList<Line> context = new LinkedList<>();
        List<String> lines;
        try {
            if (Files.exists(rootDir.resolve(fileDiff.newFile()))) {
                lines = Files.readAllLines(rootDir.resolve(fileDiff.newFile()));;
            } else {
                return new LinkedList<>();
            }
            for (int i = index - 1; i >= 0; i--) {
                String currentLine = " " + lines.get(i);
                if (lineFilter.shouldKeep(fileDiff.newFile(), i+1)) {
                    if (context.size() >= contextSize) {
                        break;
                    }
                    context.addFirst(new ContextLine(currentLine));
                }
            }
            return context;
        } catch (IOException e) {
            Logger.error("Was not able to load file:" + rootDir.resolve(fileDiff.newFile()), e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public List<Line> trailingContext(ILineFilter lineFilter, FileDiff fileDiff, int index) {
        LinkedList<Line> context = new LinkedList<>();
        List<String> lines;
        try {
            if (Files.exists(rootDir.resolve(fileDiff.oldFile()))) {
                lines = Files.readAllLines(rootDir.resolve(fileDiff.oldFile()));
            } else {
                return new LinkedList<>();
            }
            for (int i = index-1; i < lines.size(); i++) {
                String currentLine = " " + lines.get(i);
                if (lineFilter.shouldKeep(fileDiff.oldFile(), i+1)) {
                    if (context.size() >= contextSize) {
                        break;
                    }
                    context.addLast(new ContextLine(currentLine));
                }
                if (i == lines.size() - 1) {
                    // Add a meta-line stating EOF 
                    context.addLast(new MetaLine());
                }
            }
            return context;
        } catch (IOException e) {
            Logger.error("Was not able to load file:" + rootDir.resolve(fileDiff.newFile()), e);
            throw new UncheckedIOException(e);
        }
    }
}