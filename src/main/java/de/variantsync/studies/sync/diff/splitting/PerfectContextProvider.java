package de.variantsync.studies.sync.diff.splitting;

import de.variantsync.evolution.util.Logger;
import de.variantsync.studies.sync.diff.components.FileDiff;
import de.variantsync.studies.sync.diff.filter.ILineFilter;
import de.variantsync.studies.sync.diff.lines.ContextLine;
import de.variantsync.studies.sync.diff.lines.Line;
import de.variantsync.studies.sync.diff.lines.MetaLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PerfectContextProvider implements IContextProvider {
    private final Path sourceDir;
    private final Path targetDir;
    private final int contextSize;
    private final int targetPathStart;
    private PerfectContext cachedLeadContext;
    private PerfectContext cachedTrailContext;

    public PerfectContextProvider(Path sourceDir, Path targetDir) {
        // Three is the default size set in unix diff
        this(sourceDir, targetDir, 3, 0);
    }

    public PerfectContextProvider(Path sourceDir, Path targetDir, int contextSize, int targetPathStart) {
        this.sourceDir = sourceDir;
        this.targetDir = targetDir;
        this.contextSize = contextSize;
        this.targetPathStart = targetPathStart;
    }
    
    @Override
    public List<Line> leadingContext(ILineFilter lineFilter, FileDiff fileDiff, int index) {
        LinkedList<Line> context = new LinkedList<>();
        try {
            PerfectContext leadContext = loadLeadContext(fileDiff, lineFilter);
            List<String> lines = leadContext.lines();
            Map<Integer, Integer> indexMap = leadContext.indexMap();
            
            for (int i = indexMap.get(index-1); i >= 0; i--) {
                if (context.size() >= contextSize) {
                    break;
                }
                String currentLine = " " + lines.get(i);
                context.addFirst(new ContextLine(currentLine));
            }
            return context;
        } catch (IOException e) {
            Logger.error("Was not able to load file:" + sourceDir.resolve(fileDiff.newFile()), e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public List<Line> trailingContext(ILineFilter lineFilter, FileDiff fileDiff, int index) {
        LinkedList<Line> context = new LinkedList<>();
        try {
            PerfectContext trailContext = loadTrailContext(fileDiff, lineFilter);
            List<String> lines = trailContext.lines();
            Map<Integer, Integer> indexMap = trailContext.indexMap();
            for (int i = indexMap.get(index-1); i < lines.size(); i++) {
                if (context.size() >= contextSize) {
                    break;
                }
                String currentLine = " " + lines.get(i);
                context.addLast(new ContextLine(currentLine));
                
                if (i == lines.size() - 1) {
                    // Add a meta-line stating EOF 
                    context.addLast(new MetaLine());
                }
            }
            return context;
        } catch (IOException e) {
            Logger.error("Was not able to load file:" + sourceDir.resolve(fileDiff.newFile()), e);
            throw new UncheckedIOException(e);
        }
    }

    private PerfectContext loadLeadContext(FileDiff fileDiff, ILineFilter lineFilter) throws IOException {
        if (cachedLeadContext == null || !fileDiff.newFile().equals(cachedLeadContext.sourceFile())) {
            this.cachedLeadContext = getPerfectContext(fileDiff.newFile(), lineFilter);
        }
        return this.cachedLeadContext;
    }

    private PerfectContext loadTrailContext(FileDiff fileDiff, ILineFilter lineFilter) throws IOException {
        if (cachedTrailContext == null || !fileDiff.oldFile().equals(cachedTrailContext.sourceFile())) {
            this.cachedTrailContext = getPerfectContext(fileDiff.oldFile(), lineFilter);
        }
        return this.cachedTrailContext;
    }

    private PerfectContext getPerfectContext(Path sourceFile, ILineFilter lineFilter) throws IOException {
        if (Files.exists(sourceDir.resolve(sourceFile))) {
            List<String> sourceLines = Files.readAllLines(sourceDir.resolve(sourceFile));
            List<String> targetLines = Files.readAllLines(targetDir.resolve(sourceFile.subpath(targetPathStart, sourceFile.getNameCount())));
            Map<Integer, Integer> indexMap = new HashMap<>();

            int sourceIndex = 0;
            int targetIndex = 0;
            LinkedList<String> perfectLines = new LinkedList<>();
            while (true) {
                String sourceLine = null;
                if (sourceIndex < sourceLines.size()) {
                    if (lineFilter.shouldKeep(sourceFile, sourceIndex + 1)) {
                        sourceLine = sourceLines.get(sourceIndex);
                    } else {
                        indexMap.put(sourceIndex, perfectLines.size());
                        sourceIndex++;
                        continue;
                    }
                }
                String targetLine = null;
                if (targetIndex < targetLines.size()) {
                    targetLine = targetLines.get(targetIndex);
                }

                if (sourceLine != null && targetLine != null && Objects.equals(sourceLine, targetLine)) {
                    indexMap.put(sourceIndex, perfectLines.size());
                    perfectLines.add(sourceLine);
                    sourceIndex++;
                    targetIndex++;
                } else if (targetLine != null) {
                    perfectLines.add(targetLine);
                    targetIndex++;
                } else if (sourceLine != null) {
                    indexMap.put(sourceIndex, perfectLines.size());
                    perfectLines.add(sourceLine);
                    sourceIndex++;
                } else {
                    break;
                }
            }

            return new PerfectContext(perfectLines, indexMap, sourceFile);
        } else {
            return new PerfectContext(new LinkedList<>(), new HashMap<>(), sourceFile);
        }

    }


}
