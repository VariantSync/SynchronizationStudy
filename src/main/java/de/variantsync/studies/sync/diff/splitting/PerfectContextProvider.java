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

    private static List<String> readLinesChecked(Path path) throws IOException {
        if (Files.exists(path)) {
            return Files.readAllLines(path);
        } else {
            return new LinkedList<>();
        }
    }

    @Override
    public List<Line> leadingContext(ILineFilter lineFilter, FileDiff fileDiff, int index) {
        LinkedList<Line> context = new LinkedList<>();
        try {
            PerfectContext leadContext = loadLeadContext(fileDiff, lineFilter);
            List<String> lines = leadContext.lines();
            Map<Integer, Integer> indexMap = leadContext.indexMap();

            for (int i = indexMap.get(index - 1); i >= 0; i--) {
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
            for (int i = indexMap.get(index - 1); i < lines.size(); i++) {
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
            this.cachedLeadContext = getPerfectLead(fileDiff.oldFile(), fileDiff.newFile(), lineFilter);
        }
        return this.cachedLeadContext;
    }

    private PerfectContext loadTrailContext(FileDiff fileDiff, ILineFilter lineFilter) throws IOException {
        if (cachedTrailContext == null || !fileDiff.oldFile().equals(cachedTrailContext.sourceFile())) {
            this.cachedTrailContext = getPerfectTrail(fileDiff.oldFile(), lineFilter);
        }
        return this.cachedTrailContext;
    }

    private PerfectContext getPerfectLead(Path sourceFileV0, Path sourceFileV1, ILineFilter lineFilter) throws IOException {
        Path targetFile = targetDir.resolve(sourceFileV0.subpath(targetPathStart, sourceFileV0.getNameCount()));
        LinkedList<String> perfectLines = new LinkedList<>();
        Map<Integer, Integer> indexMap = new HashMap<>();

        List<String> sourceLinesV0 = readLinesChecked(sourceDir.resolve(sourceFileV0));
        List<String> sourceLinesV1 = readLinesChecked(sourceDir.resolve(sourceFileV1));
        List<String> targetLines = readLinesChecked(targetFile);

        int sourceIndexV0 = 0;
        int sourceIndexV1 = 0;
        int targetIndex = 0;
        while (true) {
            String sourceLineV0 = null;
            if (sourceIndexV0 < sourceLinesV0.size()) {
                if (lineFilter.shouldKeep(sourceFileV0, sourceIndexV0 + 1)) {
                    sourceLineV0 = sourceLinesV0.get(sourceIndexV0);
                } else {
                    sourceIndexV0++;
                }
            }
            String sourceLineV1 = null;
            if (sourceIndexV1 < sourceLinesV1.size()) {
                if (lineFilter.shouldKeep(sourceFileV1, sourceIndexV1 + 1)) {
                    sourceLineV1 = sourceLinesV1.get(sourceIndexV1);
                } else {
                    indexMap.put(sourceIndexV1, perfectLines.size()-1);
                    sourceIndexV1++;
                    continue;
                }
            }
            if (!Objects.equals(sourceLineV0, sourceLineV1)) {
                // We located an edit, which we want to skip, because it should not go into the context
                String nextV0;
                if (sourceIndexV0 + 1 < sourceLinesV0.size()) {
                    nextV0 = sourceLinesV0.get(sourceIndexV0 + 1);
                    if (Objects.equals(nextV0, sourceLineV1)) {
                        // Deletion, increase the target and source.V0 index by 1
                        sourceIndexV0++;
                        targetIndex++;
                        continue;
                    }
                }
                String nextV1;
                if (sourceIndexV1 + 1 < sourceLinesV1.size()) {
                    nextV1 = sourceLinesV1.get(sourceIndexV1 + 1);
                    if (Objects.equals(nextV1, sourceLineV0)) {
                        // Insertion, increase the source.V1 index by 1
                        indexMap.put(sourceIndexV1, perfectLines.size());
                        perfectLines.add(sourceLineV1);
                        sourceIndexV1++;
                        continue;
                    }
                }
            }

            String targetLine = null;
            if (targetIndex < targetLines.size()) {
                targetLine = targetLines.get(targetIndex);
            }

            if (sourceLineV1 != null && targetLine != null && Objects.equals(sourceLineV1, targetLine)) {
                indexMap.put(sourceIndexV1, perfectLines.size());
                perfectLines.add(sourceLineV1);
                sourceIndexV0++;
                sourceIndexV1++;
                targetIndex++;
            } else if (targetLine != null) {
                perfectLines.add(targetLine);
                targetIndex++;
            } else if (sourceLineV1 != null) {
                indexMap.put(sourceIndexV1, perfectLines.size());
                perfectLines.add(sourceLineV1);
                sourceIndexV0++;
                sourceIndexV1++;
            } else {
                break;
            }
        }

        return new PerfectContext(perfectLines, indexMap, sourceFileV1);
    }

    private PerfectContext getPerfectTrail(Path sourceFile, ILineFilter lineFilter) throws IOException {
        Path targetFile = targetDir.resolve(sourceFile.subpath(targetPathStart, sourceFile.getNameCount()));
        LinkedList<String> perfectLines = new LinkedList<>();
        Map<Integer, Integer> indexMap = new HashMap<>();

        List<String> sourceLines = readLinesChecked(sourceDir.resolve(sourceFile));
        List<String> targetLines = readLinesChecked(targetFile);

        int sourceIndex = 0;
        int targetIndex = 0;
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
    }
}
