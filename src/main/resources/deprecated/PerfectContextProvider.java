//package de.variantsync.studies.sync.diff.splitting;
//
//import de.variantsync.evolution.util.Logger;
//import de.variantsync.studies.sync.diff.components.FileDiff;
//import de.variantsync.studies.sync.diff.filter.ILineFilter;
//import de.variantsync.studies.sync.diff.lines.ContextLine;
//import de.variantsync.studies.sync.diff.lines.Line;
//import de.variantsync.studies.sync.diff.lines.MetaLine;
//
//import java.io.IOException;
//import java.io.UncheckedIOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.*;
//
//public class PerfectContextProvider implements IContextProvider {
//    private final Path sourceDir;
//    private final Path targetDir;
//    private final int contextSize;
//    private final int targetPathStart;
//    private PerfectContext cachedLeadContext;
//    private PerfectContext cachedTrailContext;
//
//    public PerfectContextProvider(Path sourceDir, Path targetDir) {
//        // Three is the default size set in unix diff
//        this(sourceDir, targetDir, 3, 0);
//    }
//
//    public PerfectContextProvider(Path sourceDir, Path targetDir, int contextSize, int targetPathStart) {
//        this.sourceDir = sourceDir;
//        this.targetDir = targetDir;
//        this.contextSize = contextSize;
//        this.targetPathStart = targetPathStart;
//    }
//
//    private static List<String> readLinesChecked(Path path) throws IOException {
//        if (Files.exists(path)) {
//            return Files.readAllLines(path);
//        } else {
//            return new LinkedList<>();
//        }
//    }
//
//    @Override
//    public List<Line> leadingContext(ILineFilter lineFilter, FileDiff fileDiff, int index) {
//        LinkedList<Line> context = new LinkedList<>();
//        try {
//            PerfectContext leadContext = loadLeadContext(fileDiff, lineFilter);
//            List<String> lines = leadContext.lines();
//            Map<Integer, Integer> indexMap = leadContext.indexMap();
////            for (String line : lines) {
////                System.out.println(line);
////            }
//            for (int i = indexMap.get(index - 1); i >= 0; i--) {
//                if (context.size() >= contextSize) {
//                    break;
//                }
//                String currentLine = " " + lines.get(i);
//                context.addFirst(new ContextLine(currentLine));
//            }
//            return context;
//        } catch (IOException e) {
//            Logger.error("Was not able to load file:" + sourceDir.resolve(fileDiff.newFile()), e);
//            throw new UncheckedIOException(e);
//        }
//    }
//
//    @Override
//    public List<Line> trailingContext(ILineFilter lineFilter, FileDiff fileDiff, int index) {
//        LinkedList<Line> context = new LinkedList<>();
//        try {
//            PerfectContext trailContext = loadTrailContext(fileDiff, lineFilter);
//            List<String> lines = trailContext.lines();
//            Map<Integer, Integer> indexMap = trailContext.indexMap();
//            for (int i = indexMap.get(index - 1); i < lines.size(); i++) {
//                if (context.size() >= contextSize) {
//                    break;
//                }
//                String currentLine = " " + lines.get(i);
//                context.addLast(new ContextLine(currentLine));
//
//                if (i == lines.size() - 1) {
//                    // Add a meta-line stating EOF 
//                    context.addLast(new MetaLine());
//                }
//            }
//            return context;
//        } catch (IOException e) {
//            Logger.error("Was not able to load file:" + sourceDir.resolve(fileDiff.newFile()), e);
//            throw new UncheckedIOException(e);
//        }
//    }
//
//    private PerfectContext loadLeadContext(FileDiff fileDiff, ILineFilter lineFilter) throws IOException {
//        if (cachedLeadContext == null || !fileDiff.newFile().equals(cachedLeadContext.sourceFile())) {
//            this.cachedLeadContext = perfectLead(fileDiff.oldFile(), fileDiff.newFile(), lineFilter);
//        }
//        return this.cachedLeadContext;
//    }
//
//    private PerfectContext loadTrailContext(FileDiff fileDiff, ILineFilter lineFilter) throws IOException {
//        if (cachedTrailContext == null || !fileDiff.oldFile().equals(cachedTrailContext.sourceFile())) {
//            this.cachedTrailContext = getPerfectTrail(fileDiff.oldFile(), lineFilter);
//        }
//        return this.cachedTrailContext;
//    }
//
//    private PerfectContext getPerfectTrail(Path sourceFile, ILineFilter lineFilter) throws IOException {
//        Path targetFile = targetDir.resolve(sourceFile.subpath(targetPathStart, sourceFile.getNameCount()));
//        LinkedList<String> perfectLines = new LinkedList<>();
//        Map<Integer, Integer> indexMap = new HashMap<>();
//
//        List<String> sourceLines = readLinesChecked(sourceDir.resolve(sourceFile));
//        List<String> targetLines = readLinesChecked(targetFile);
//
//        int sourceIndex = 0;
//        int targetIndex = 0;
//        int perfectIndex = 0;
//        while (true) {
//            String sourceLine = null;
//            if (sourceIndex < sourceLines.size()) {
//                if (lineFilter.shouldKeep(sourceFile, sourceIndex + 1)) {
//                    sourceLine = sourceLines.get(sourceIndex);
//                } else {
//                    indexMap.put(sourceIndex, perfectIndex);
//                    sourceIndex++;
//                    continue;
//                }
//            }
//            String targetLine = null;
//            if (targetIndex < targetLines.size()) {
//                targetLine = targetLines.get(targetIndex);
//            }
//
//            if (sourceLine != null && targetLine != null && Objects.equals(sourceLine, targetLine)) {
//                indexMap.put(sourceIndex, perfectIndex);
//                perfectLines.add(sourceLine);
//                perfectIndex = perfectLines.size();
//                sourceIndex++;
//                targetIndex++;
//            } else if (targetLine != null) {
//                perfectLines.add(targetLine);
//                targetIndex++;
//            } else if (sourceLine != null) {
//                indexMap.put(sourceIndex, perfectIndex);
//                perfectIndex++;
//                perfectLines.add(sourceLine);
//                sourceIndex++;
//            } else {
//                break;
//            }
//        }
//
//        return new PerfectContext(perfectLines, indexMap, sourceFile);
//    }
//
//    private PerfectContext perfectLead(Path sourceFileV0, Path sourceFileV1, ILineFilter lineFilter) throws IOException {
//        Path targetFile = targetDir.resolve(sourceFileV0.subpath(targetPathStart, sourceFileV0.getNameCount()));
//        LinkedList<String> perfectLines = new LinkedList<>();
//        Map<Integer, Integer> indexMap = new HashMap<>();
//
//        Map<Integer, Boolean> targetOnlyLine = determineTargetOnly(sourceFileV0, targetFile, lineFilter);
//        List<String> sourceLinesV1 = readLinesChecked(sourceDir.resolve(sourceFileV1));
//        List<String> targetLines = readLinesChecked(targetFile);
//
//        int sourceIndex = 0;
//        int targetIndex = 0;
//        // We require a frozen state of the target index, to check equality in case of multiple edits in the source
//        int frozenSourceIndex = 0;
//        int frozenTargetIndex = 0;
//        int perfectIndex = 0;
//        while (true) {
//            String sourceLine = null;
//            if (sourceIndex < sourceLinesV1.size()) {
//                if (lineFilter.shouldKeep(sourceFileV1, sourceIndex + 1)) {
//                    sourceLine = sourceLinesV1.get(sourceIndex);
//                } else {
//                    indexMap.put(sourceIndex, perfectIndex - 1);
//                    sourceIndex++;
//                    continue;
//                }
//            }
//
//            String frozenSource = null;
//            if (frozenSourceIndex < sourceLinesV1.size()) {
//                frozenSource = sourceLinesV1.get(frozenSourceIndex);
//            }
//
//            String targetLine = null;
//            if (targetIndex < targetLines.size()) {
//                targetLine = targetLines.get(targetIndex);
//            }
//
//            String frozenTarget = null;
//            if (frozenTargetIndex < targetLines.size()) {
//                frozenTarget = targetLines.get(frozenTargetIndex);
//            }
//
//            if (sourceLine != null && targetLine != null && Objects.equals(sourceLine, targetLine)) {
//                perfectIndex = perfectLines.size();
//                indexMap.put(sourceIndex, perfectIndex);
//                perfectLines.add(sourceLine);
//                sourceIndex++;
//                targetIndex++;
//                // Update the frozen index
//                frozenSourceIndex = sourceIndex;
//                frozenTargetIndex = targetIndex;
//            } else if (targetLine != null && targetOnlyLine.get(targetIndex)) {
//                // The lines are different, because the target line only exists in the target
//                indexMap.put(sourceIndex, perfectIndex);
//                perfectLines.add(targetLine);
//                targetIndex++;
//            } else if (frozenTarget != null && Objects.equals(sourceLine, frozenTarget)) {
//                indexMap.put(sourceIndex, perfectIndex);
//                perfectLines.add(sourceLine);
//                sourceIndex++;
//                targetIndex = frozenTargetIndex + 1;
//                // Update the frozen index
//                frozenTargetIndex = targetIndex;
//            } else if (frozenSource != null && Objects.equals(frozenSource, targetLine)) {
//                // Skip this line, it was added before
//                targetIndex++;
//                frozenSourceIndex++;
//            } else if (sourceLine != null) {
//                // The lines are different, because the source line was edited
//                indexMap.put(sourceIndex, perfectIndex);
//                perfectLines.add(sourceLine);
//                sourceIndex++;
//                targetIndex++;
//            } else {
//                break;
//            }
//        }
//
//        return new PerfectContext(perfectLines, indexMap, sourceFileV1);
//    }
//
//    private Map<Integer, Boolean> determineTargetOnly(Path sourceFileV0, Path targetFile, ILineFilter lineFilter) throws IOException {
//        Map<Integer, Boolean> targetOnlyMap = new HashMap<>();
//
//        List<String> sourceLinesV0 = readLinesChecked(sourceDir.resolve(sourceFileV0));
//        List<String> targetLines = readLinesChecked(targetFile);
//
//        int sourceIndexV0 = 0;
//        int targetIndex = 0;
//        while (true) {
//            String sourceLineV0 = null;
//            if (sourceIndexV0 < sourceLinesV0.size()) {
//                if (lineFilter.shouldKeep(sourceFileV0, sourceIndexV0 + 1)) {
//                    sourceLineV0 = sourceLinesV0.get(sourceIndexV0);
//                } else {
//                    sourceIndexV0++;
//                    continue;
//                }
//            }
//
//            String targetLine = null;
//            if (targetIndex < targetLines.size()) {
//                targetLine = targetLines.get(targetIndex);
//            }
//
//            if (sourceLineV0 != null && targetLine != null && Objects.equals(sourceLineV0, targetLine)) {
//                targetOnlyMap.put(targetIndex, false);
//                sourceIndexV0++;
//                targetIndex++;
//            } else if (targetLine != null) {
//                targetOnlyMap.put(targetIndex, true);
//                targetIndex++;
//            } else {
//                break;
//            }
//        }
//        return targetOnlyMap;
//    }
//}
