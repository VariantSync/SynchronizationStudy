package de.variantsync.studies.sync.experiment;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.variantsync.evolution.util.Logger;
import de.variantsync.evolution.variability.SPLCommit;
import de.variantsync.studies.sync.diff.components.FineDiff;
import de.variantsync.studies.sync.diff.components.OriginalDiff;
import de.variantsync.studies.sync.diff.lines.AddedLine;
import de.variantsync.studies.sync.diff.lines.Line;
import de.variantsync.studies.sync.diff.lines.RemovedLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ResultAnalysis {
    static Path resultPath = Path.of("empirical-study").toAbsolutePath().resolve("results.txt");

    public static PatchOutcome processOutcome(final String dataset,
                                              final long runID,
                                              final String sourceVariant,
                                              final String targetVariant,
                                              final SPLCommit commitV0, final SPLCommit commitV1,
                                              final FineDiff normalPatch, final FineDiff filteredPatch,
                                              final FineDiff resultDiffNormal, final FineDiff resultDiffFiltered,
                                              final OriginalDiff rejectsNormal, final OriginalDiff rejectsFiltered,
                                              final FineDiff evolutionDiff,
                                              final List<Path> skippedFilesNormal) {
        // evaluate patch rejects
        final int fileNormal = new HashSet<>(normalPatch.content().stream().map(fd -> fd.oldFile().toString()).collect(Collectors.toList())).size();
        final int lineNormal = normalPatch.content().stream().mapToInt(fd -> fd.hunks().size()).sum();
        int fileNormalFailed = 0;
        int lineNormalFailed = 0;
        if (rejectsNormal != null) {
            Logger.status("Commit-sized patch failed");

            fileNormalFailed = new HashSet<>(rejectsNormal.fileDiffs().stream().map(fd -> fd.oldFile().toString()).collect(Collectors.toList())).size();
            Logger.status("" + fileNormalFailed + " of " + fileNormal + " normal file-sized patches failed.");
            lineNormalFailed = rejectsNormal.fileDiffs().stream().mapToInt(fd -> fd.hunks().size()).sum();
            Logger.status("" + lineNormalFailed + " of " + lineNormal + " normal line-sized patches failed");
        } else {
            Logger.status("Commit-sized patch succeeded.");
        }

        final int fileFiltered = new HashSet<>(filteredPatch.content().stream().map(fd -> fd.oldFile().toString()).collect(Collectors.toList())).size();
        final int lineFiltered = filteredPatch.content().stream().mapToInt(fd -> fd.hunks().size()).sum();
        int fileFilteredFailed = 0;
        int lineFilteredFailed = 0;
        if (rejectsFiltered != null) {
            fileFilteredFailed = new HashSet<>(rejectsFiltered.fileDiffs().stream().map(fd -> fd.oldFile().toString()).collect(Collectors.toList())).size();
            Logger.status("" + fileFilteredFailed + " of " + fileFiltered + " filtered file-sized patches failed.");
            lineFilteredFailed = rejectsFiltered.fileDiffs().stream().mapToInt(fd -> fd.hunks().size()).sum();
            Logger.status("" + lineFilteredFailed + " of " + lineFiltered + " filtered line-sized patches failed");
        }

        int selected = lineNormal;
        selected -= lineNormalFailed;
        selected -= normalPatch.content().stream().filter(fd -> skippedFilesNormal.contains(fd.oldFile())).mapToInt(fd -> fd.hunks().size()).sum();

        final ConditionTable normalConditionTable = calculateConditionTable(normalPatch, filteredPatch, resultDiffNormal, evolutionDiff);
        final long normalTP = normalConditionTable.tp;
        final long normalFP = normalConditionTable.fp;
        final long normalTN = normalConditionTable.tn;
        final long normalFN = normalConditionTable.fn;

//        // false negatives = relevant that failed
////        final int normalFN = lineFilteredFailed;
//        final long normalFN = resultDiffNormal.content().stream().flatMap(fd -> fd.hunks().stream()).flatMap(hunk -> hunk.content().stream()).filter(l -> l instanceof AddedLine).count();
//        // true positives = relevant - false negatives
////        final int normalTP = lineFiltered - normalFN;
//        final long normalTP = lineFiltered - normalFN;
//        // false positive = selected - true positive
////        final int normalFP = selected - normalTP;
//        final long normalFP = resultDiffNormal.content().stream().flatMap(fd -> fd.hunks().stream()).flatMap(hunk -> hunk.content().stream()).filter(l -> l instanceof RemovedLine).count();
//        // true negative = all - relevant - false positive
////        final int normalTN = lineNormal - lineFiltered - normalFP;
//        final long normalTN = lineNormal - lineFiltered - normalFP;

//        final int filteredTP = lineFiltered - lineFilteredFailed;
//        final int filteredFP = 0;
//        final int filteredTN = lineNormal - lineFiltered;
//        final int filteredFN = lineFilteredFailed;
//        final long filteredFP = ResultDiffFiltered.content().stream().flatMap(fd -> fd.hunks().stream()).flatMap(hunk -> hunk.content().stream()).filter(l -> l instanceof RemovedLine).count();
//        final long filteredTN = lineNormal - lineFiltered - filteredFP;
//        final long filteredFN = ResultDiffFiltered.content().stream().flatMap(fd -> fd.hunks().stream()).flatMap(hunk -> hunk.content().stream()).filter(l -> l instanceof AddedLine).count();
//        final long filteredTP = lineFiltered - filteredFN;

        final ConditionTable filteredConditionTable = calculateConditionTable(filteredPatch, filteredPatch, resultDiffFiltered, evolutionDiff);
        final long filteredTP = filteredConditionTable.tp;
        final long filteredFP = filteredConditionTable.fp;
        final long filteredTN = filteredConditionTable.tn + (lineNormal - lineFiltered);
        final long filteredFN = filteredConditionTable.fn;

        assert normalTP + normalFP + normalFN + normalTN == filteredTP + filteredFP + filteredTN + filteredFN;
        assert filteredTP + filteredFN == lineFiltered;
        assert normalTP + normalFN == lineFiltered;
        assert filteredFP + filteredTN == lineNormal - lineFiltered;
        assert normalFP + normalTN == lineNormal - lineFiltered;
        assert normalTP <= filteredTP;
        assert normalTN <= filteredTN;
        if (filteredFP > 0) {
            System.out.println("Special case.");
        }

        return new PatchOutcome(dataset,
                runID,
                commitV0.id(),
                commitV1.id(),
                sourceVariant,
                targetVariant,
                resultDiffNormal.content().size(),
                resultDiffFiltered.content().size(),
                fileNormal,
                lineNormal,
                fileNormal - fileNormalFailed,
                lineNormal - lineNormalFailed,
                fileFiltered,
                lineFiltered,
                fileFiltered - fileFilteredFailed,
                lineFiltered - lineFilteredFailed,
                normalTP,
                normalFP,
                normalTN,
                normalFN,
                filteredTP,
                filteredFP,
                filteredTN,
                filteredFN);
    }

    private static record ConditionTable(long tp, long fp, long tn, long fn) {}

    private static ConditionTable calculateConditionTable(FineDiff patch, FineDiff filteredPatch, FineDiff resultDiff, FineDiff evolutionDiff) {
        List<Line> changesInPatch = FineDiff.determineChangedLines(patch);
        List<Line> changesInFilteredPatch = FineDiff.determineChangedLines(filteredPatch);
        List<Line> changesInResult = FineDiff.determineChangedLines(resultDiff);
        List<Line> changesInEvolution = FineDiff.determineChangedLines(evolutionDiff);

        // Determine changes in the target variant's evolution that cannot be synchronized, because they are not part of the source variant and therefore not of the patch
        final List<Line> unsynchronizableChanges = new LinkedList<>();
        {
            final List<Line> tempChanges = new LinkedList<>(changesInFilteredPatch);
            for (Line evolutionChange : changesInEvolution) {
                if (!tempChanges.contains(evolutionChange)) {
                    unsynchronizableChanges.add(evolutionChange);
                }
            }
        }

        // Determine expected changes, i.e., changes in the target variant's evolution that can be synchronized
        final List<Line> expectedChanges = new LinkedList<>(changesInEvolution);
        expectedChanges.removeAll(unsynchronizableChanges);

        // Determine actual differences between result and expected result, i.e., changes that should have been synchronized but were not
        final List<Line> actualDifferences = new LinkedList<>(changesInResult);
        actualDifferences.removeAll(unsynchronizableChanges);

        // We first want to account for all actual differences that should not have been there, these can either be
        // classified into false positive or false negative
        long fp = 0;
        long fn = 0;
        for (Line actualDifference : actualDifferences) {
            // Is it a false negative?
            if (changesInPatch.contains(actualDifference)) {
                fn++;
                // Each line in the patch must only be considered once, all additional difference are false positives
                changesInPatch.remove(actualDifference);
            } else {
                // It must be a false positive
                String changedText = actualDifference.line().substring(1);
                Line foundLine = null;
                for (Line patchLine : changesInPatch) {
                    if (patchLine.line().substring(1).equals(changedText)) {
                        foundLine = patchLine;
                    }
                }
                if (foundLine == null) {
                    throw new IllegalStateException();
                }
                changesInPatch.remove(foundLine);
                fp++;
            }
        }

        // Now account for the remaining lines in the patch file and determine whether they are true positive or true negative
        long tp = 0;
        long tn = 0;
        for (var patchLine : changesInPatch) {
            if (expectedChanges.contains(patchLine)) {
                // In Patch & Expected: It is a true positive
                tp++;
            } else {
                // In Patch & Unexpected: It is a true negative
                tn++;
            }
            // Remove the line from the expected changes to account for similar changes
            expectedChanges.remove(patchLine);
        }
        return new ConditionTable(tp, fp, tn, fn);
    }

    public static void main(final String... args) throws IOException {
        final List<PatchOutcome> allOutcomes = loadResultObjects(resultPath);
        System.out.println();
        System.out.println("++++++++++++++++++++++++++++++++++++++");
        System.out.println("Patch Success");
        System.out.println("++++++++++++++++++++++++++++++++++++++");
        printTechnicalSuccess(allOutcomes);

        System.out.println();
        System.out.println("++++++++++++++++++++++++++++++++++++++");
        System.out.println("Precision / Recall");
        System.out.println("++++++++++++++++++++++++++++++++++++++");

        printPrecisionRecall(allOutcomes.stream().mapToLong(PatchOutcome::normalTP).sum(),
                allOutcomes.stream().mapToLong(PatchOutcome::normalFP).sum(),
                allOutcomes.stream().mapToLong(PatchOutcome::normalTN).sum(),
                allOutcomes.stream().mapToLong(PatchOutcome::normalFN).sum());

        System.out.println();
        System.out.println("++++++++++++++++++++++++++++++++++++++");
        System.out.println();

        printPrecisionRecall(allOutcomes.stream().mapToLong(PatchOutcome::filteredTP).sum(),
                allOutcomes.stream().mapToLong(PatchOutcome::filteredFP).sum(),
                allOutcomes.stream().mapToLong(PatchOutcome::filteredTN).sum(),
                allOutcomes.stream().mapToLong(PatchOutcome::filteredFN).sum());

        System.out.println();
        System.out.println("++++++++++++++++++++++++++++++++++++++");
        System.out.println("Actual vs. Expected");
        System.out.println("++++++++++++++++++++++++++++++++++++++");

        final long countNormalAsExpected = allOutcomes.stream().mapToLong(PatchOutcome::countOfNormalAsExpected).sum();
        final long countFilteredAsExpected = allOutcomes.stream().mapToLong(PatchOutcome::countOfFilteredAsExpected).sum();
        final long countNormal = allOutcomes.stream().mapToLong(PatchOutcome::lineNormal).sum();
        final long countFiltered = allOutcomes.stream().mapToLong(PatchOutcome::lineFiltered).sum();
        System.out.printf("Normal patching achieved the expected result %d out of %d times.%n", countNormalAsExpected, countNormal);
        System.out.printf("Filtered patching achieved the expected result %d out of %d times.%n", countFilteredAsExpected, countFiltered);
        
        System.out.println("++++++++++++++++++++++++++++++++++++++");
        System.out.println("++++++++++++++++++++++++++++++++++++++");
    }

    private static void printTechnicalSuccess(final List<PatchOutcome> allOutcomes) {
        final long commitPatches = allOutcomes.size();
        final long commitSuccess = allOutcomes.stream().filter(o -> o.lineSuccessNormal() == o.lineNormal()).count();
        System.out.printf("%d of %d commit-sized patch applications succeeded (%s)%n", commitSuccess, commitPatches, percentage(commitSuccess, commitPatches));

        final long fileNormal = allOutcomes.stream().mapToLong(PatchOutcome::fileNormal).sum();
        final long fileSuccessNormal = allOutcomes.stream().mapToLong(PatchOutcome::fileSuccessNormal).sum();

        System.out.printf("%d of %d file-sized patch applications succeeded (%s)%n", fileSuccessNormal, fileNormal, percentage(fileSuccessNormal, fileNormal));

        final long lineNormal = allOutcomes.stream().mapToLong(PatchOutcome::lineNormal).sum();
        final long lineSuccessNormal = allOutcomes.stream().mapToLong(PatchOutcome::lineSuccessNormal).sum();
        System.out.printf("%d of %d line-sized patch applications succeeded (%s)%n", lineSuccessNormal, lineNormal, percentage(lineSuccessNormal, lineNormal));

        final long fileFiltered = allOutcomes.stream().mapToLong(PatchOutcome::fileFiltered).sum();
        final long fileSuccessFiltered = allOutcomes.stream().mapToLong(PatchOutcome::fileSuccessFiltered).sum();
        System.out.printf("%d of %d filtered file-sized patch applications succeeded (%s)%n", fileSuccessFiltered, fileFiltered, percentage(fileSuccessFiltered, fileFiltered));

        final long lineFiltered = allOutcomes.stream().mapToLong(PatchOutcome::lineFiltered).sum();
        final long lineSuccessFiltered = allOutcomes.stream().mapToLong(PatchOutcome::lineSuccessFiltered).sum();
        System.out.printf("%d of %d filtered line-sized patch applications succeeded (%s)%n", lineSuccessFiltered, lineFiltered, percentage(lineSuccessFiltered, lineFiltered));

    }

    private static void printPrecisionRecall(final long tp, final long fp, final long tn, final long fn) {
        final double precision = (double) tp / ((double) tp + fp);
        final double recall = (double) tp / ((double) tp + fn);
        final double f_measure = (2 * precision * recall) / (precision + recall);

        System.out.println("TP: " + tp);
        System.out.println("FP: " + fp);
        System.out.println("TN: " + tn);
        System.out.println("FN: " + fn);
        System.out.printf("Precision: %1.2f%n", precision);
        System.out.printf("Recall: %1.2f%n", recall);
        System.out.printf("F-Measure: %1.2f%n", f_measure);
    }

    public static List<PatchOutcome> loadResultObjects(final Path path) throws IOException {
        final List<PatchOutcome> results = new LinkedList<>();
        final List<String> lines = Files.readAllLines(path);
        List<String> currentResult = new LinkedList<>();
        for (final String l : lines) {
            if (l.isEmpty()) {
                results.add(parseResult(currentResult));
                currentResult = new LinkedList<>();
            } else {
                currentResult.add(l);
            }
        }
        return results;
    }

    public static PatchOutcome parseResult(final List<String> lines) {
        final Gson gson = new Gson();
        final StringBuilder sb = new StringBuilder();
        lines.forEach(l -> sb.append(l).append("\n"));
        final JsonObject object = gson.fromJson(sb.toString(), JsonObject.class);
        return PatchOutcome.FromJSON(object);
    }

    public static String percentage(final long x, final long y) {
        final double percentage;
        if (y == 0) {
            percentage = 0;
        } else {
            percentage = 100 * ((double) x / (double) y);
        }
        return String.format("%3.1f%s", percentage, "%");
    }
}
