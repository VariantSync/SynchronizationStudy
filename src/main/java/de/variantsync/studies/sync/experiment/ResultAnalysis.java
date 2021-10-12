package de.variantsync.studies.sync.experiment;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.variantsync.evolution.util.Logger;
import de.variantsync.evolution.variability.SPLCommit;
import de.variantsync.studies.sync.diff.components.FineDiff;
import de.variantsync.studies.sync.diff.components.OriginalDiff;
import de.variantsync.studies.sync.diff.lines.AddedLine;
import de.variantsync.studies.sync.diff.lines.Change;
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
                                              final List<Path> skippedFilesNormal,
                                              final List<Path> skippedFilesFiltered) {
        // evaluate patch rejects
        final int fileNormal = new HashSet<>(normalPatch.content().stream().map(fd -> fd.oldFile().toString()).collect(Collectors.toList())).size();
        final int lineNormal = normalPatch.content().stream().mapToInt(fd -> fd.hunks().size()).sum();
        var temp = normalPatch.content().stream().filter(fd -> skippedFilesNormal.contains(fd.oldFile())).mapToInt(fd -> fd.hunks().size()).sum();
        int fileNormalFailed = 0;
        int lineNormalFailed = 0;
        if (rejectsNormal != null) {
            Logger.status("Commit-sized patch failed");

            fileNormalFailed = new HashSet<>(rejectsNormal.fileDiffs().stream().map(fd -> fd.oldFile().toString()).collect(Collectors.toList())).size();
            fileNormalFailed += normalPatch.content().stream().filter(fd -> skippedFilesNormal.contains(fd.oldFile())).count();
            Logger.status("" + fileNormalFailed + " of " + fileNormal + " normal file-sized patches failed.");
            lineNormalFailed = rejectsNormal.fileDiffs().stream().mapToInt(fd -> fd.hunks().size()).sum();
            lineNormalFailed += normalPatch.content().stream().filter(fd -> skippedFilesNormal.contains(fd.oldFile())).mapToInt(fd -> fd.hunks().size()).sum();
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
            fileNormalFailed += filteredPatch.content().stream().filter(fd -> skippedFilesFiltered.contains(fd.oldFile())).count();
            Logger.status("" + fileFilteredFailed + " of " + fileFiltered + " filtered file-sized patches failed.");
            lineFilteredFailed = rejectsFiltered.fileDiffs().stream().mapToInt(fd -> fd.hunks().size()).sum();
            lineNormalFailed += filteredPatch.content().stream().filter(fd -> skippedFilesFiltered.contains(fd.oldFile())).mapToInt(fd -> fd.hunks().size()).sum();
            Logger.status("" + lineFilteredFailed + " of " + lineFiltered + " filtered line-sized patches failed");
        }

        final ConditionTable normalConditionTable = calculateConditionTable(normalPatch, normalPatch, resultDiffNormal, evolutionDiff);
        final long normalTP = normalConditionTable.tpCount();
        final long normalFP = normalConditionTable.fpCount();
        final long normalTN = normalConditionTable.tnCount();
        final long normalFN = normalConditionTable.fnCount();
        final long normalWrongLocation = normalConditionTable.wrongLocationCount();

        final ConditionTable filteredConditionTable = calculateConditionTable(filteredPatch, normalPatch, resultDiffFiltered, evolutionDiff);
        final long filteredTP = filteredConditionTable.tpCount();
        final long filteredFP = filteredConditionTable.fpCount();
        final long filteredTN = filteredConditionTable.tnCount();
        final long filteredFN = filteredConditionTable.fnCount();
        final long filteredWrongLocation = filteredConditionTable.wrongLocationCount();

        assert normalTP + normalFP + normalFN + normalTN == filteredTP + filteredFP + filteredTN + filteredFN;
        assert filteredTP + filteredFP + filteredFN + filteredTN == lineFiltered + (lineNormal - lineFiltered);

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
                normalWrongLocation,
                filteredTP,
                filteredFP,
                filteredTN,
                filteredFN,
                filteredWrongLocation);
    }

    private static ConditionTable calculateConditionTable(FineDiff evaluatedPatch, FineDiff unfilteredPatch, FineDiff resultDiff, FineDiff evolutionDiff) {
        List<Change> changesInPatch = FineDiff.determineChangedLines(evaluatedPatch);
        List<Change> changesInUnfilteredPatch = FineDiff.determineChangedLines(unfilteredPatch);
        List<Change> changesInResult = FineDiff.determineChangedLines(resultDiff);
        List<Change> changesInEvolution = FineDiff.determineChangedLines(evolutionDiff);

        // Determine changes in the target variant's evolution that cannot be synchronized, because they are not part of the source variant and therefore not of the patch
        final List<Change> unsynchronizableChanges = new LinkedList<>();
        {
            final List<Change> tempChanges = new LinkedList<>(changesInUnfilteredPatch);
            for (Change evolutionChange : changesInEvolution) {
                if (!tempChanges.contains(evolutionChange)) {
                    unsynchronizableChanges.add(evolutionChange);
                } else {
                    tempChanges.remove(evolutionChange);
                }
            }
        }

        // Determine expected changes, i.e., changes in the target variant's evolution that can be synchronized
        final List<Change> expectedChanges = new LinkedList<>(changesInEvolution);
        unsynchronizableChanges.forEach(expectedChanges::remove);

        // Determine actual differences between result and expected result,
        // i.e., changes that should have been synchronized but were not or changes that should not have been synchronized
        final List<Change> actualDifferences = new LinkedList<>(changesInResult);
        unsynchronizableChanges.forEach(actualDifferences::remove);

        assert changesInUnfilteredPatch.size() >= changesInPatch.size();
        assert changesInEvolution.size() - changesInUnfilteredPatch.size() <= unsynchronizableChanges.size();
        assert changesInEvolution.size() - unsynchronizableChanges.size() <= changesInUnfilteredPatch.size();
        // We first want to account for all actual differences that should not have been there, these can either be
        // classified into false positive or false negative
        List<Change> fpChanges = new LinkedList<>();
        List<Change> fnChanges = new LinkedList<>();
        List<Change> remainingDifferences = new LinkedList<>();
        for (Change actualDifference : actualDifferences) {
            // Is it a false negative?
            if (expectedChanges.contains(actualDifference)) {
                fnChanges.add(actualDifference);
                // Each line in the patch must only be considered once, all additional difference are false positives
                changesInUnfilteredPatch.remove(actualDifference);
                changesInPatch.remove(actualDifference);
                expectedChanges.remove(actualDifference);
            } else {
                // It was not a false negative, so it should later be checked whether it is a false positive
                remainingDifferences.add(actualDifference);
            }
        }
        // Now account for the remaining differences between the actual and the expected result. They are either
        // false positives, i.e. changes in the patch that were applied but should not have been, or the mirror change
        // of a false negative that was applied to the wrong location.
        List<Change> wrongLocation = new LinkedList<>();
        for (Change remainingDifference : remainingDifferences) {
            String changedText = remainingDifference.line().line().substring(1);
            Change foundLine = null;
            for (Change patchLine : changesInPatch) {
                if (patchLine.line().line().substring(1).equals(changedText)) {
                    foundLine = patchLine;
                }
            }
            if (foundLine != null) {
                // The patch contained a false positive, as the difference was not expected.
                changesInPatch.remove(foundLine);
                changesInUnfilteredPatch.remove(foundLine);
                fpChanges.add(remainingDifference);
            } else {
                // This case happens if a line has been synchronized, but to the wrong location. This will result
                // in the actual differences containing the line once to be added and once to be removed. Therefore,
                // it has already been removed from the changes in the patch.
                Change tempChange;
                if (remainingDifference.line() instanceof AddedLine) {
                    tempChange = new Change(remainingDifference.file(), new RemovedLine("-" + changedText));
                } else {
                    tempChange = new Change(remainingDifference.file(), new AddedLine("+" + changedText));
                }
                if (changesInPatch.contains(tempChange)) {
                    wrongLocation.add(tempChange);
                    changesInPatch.remove(tempChange);
                }
            }
        }

        // Now account for the remaining lines in the patch file and determine whether they are true positive or true negative
        List<Change> tpChanges = new LinkedList<>();
        List<Change> tnChanges = new LinkedList<>();
        for (var patchLine : changesInUnfilteredPatch) {
            if (expectedChanges.contains(patchLine)) {
                // In Patch & Expected: It is a true positive
                tpChanges.add(patchLine);
            } else {
                // In Patch & Unexpected: It is a true negative
                tnChanges.add(patchLine);
            }
            // Remove the line from the expected changes to account for similar changes
            expectedChanges.remove(patchLine);
        }
        long tp = tpChanges.size();
        long fp = fpChanges.size();
        long tn = tnChanges.size();
        long fn = fnChanges.size();
        assert tp + fp + tn + fn == FineDiff.determineChangedLines(unfilteredPatch).size();
        return new ConditionTable(tpChanges, fpChanges, tnChanges, fnChanges, wrongLocation);
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

        long normalTP = allOutcomes.stream().mapToLong(PatchOutcome::normalTP).sum();
        long normalFP = allOutcomes.stream().mapToLong(PatchOutcome::normalFP).sum();
        long normalTN = allOutcomes.stream().mapToLong(PatchOutcome::normalTN).sum();
        long normalFN = allOutcomes.stream().mapToLong(PatchOutcome::normalFN).sum();
        long normalWrongLocation = allOutcomes.stream().mapToLong(PatchOutcome::normalWrongLocation).sum();

        printPrecisionRecall(normalTP,
                normalFP,
                normalTN,
                normalFN,
                normalWrongLocation);


        System.out.println();
        System.out.println("++++++++++++++++++++++++++++++++++++++");
        System.out.println();

        long filteredTP = allOutcomes.stream().mapToLong(PatchOutcome::filteredTP).sum();
        long filteredFP = allOutcomes.stream().mapToLong(PatchOutcome::filteredFP).sum();
        long filteredTN = allOutcomes.stream().mapToLong(PatchOutcome::filteredTN).sum();
        long filteredFN = allOutcomes.stream().mapToLong(PatchOutcome::filteredFN).sum();
        long filteredWrongLocation = allOutcomes.stream().mapToLong(PatchOutcome::filteredWrongLocation).sum();

        printPrecisionRecall(filteredTP,
                filteredFP,
                filteredTN,
                filteredFN,
                filteredWrongLocation);

        System.out.println();
        System.out.println("++++++++++++++++++++++++++++++++++++++");
        System.out.println("Actual vs. Expected");
        System.out.println("++++++++++++++++++++++++++++++++++++++");

        long expectedCountNormal = normalTP + normalTN;
        long allNormal = normalTP + normalFP + normalTN + normalFN;
        System.out.printf("Normal patching achieved the expected result %d out of %d times (Accuracy: %s).%n", expectedCountNormal, allNormal, percentage(expectedCountNormal, allNormal));
        long expectedCountFiltered = filteredTP + filteredTN;
        long allFiltered = filteredTP + filteredFP + filteredTN + filteredFN;
        System.out.printf("Filtered patching achieved the expected result %d out of %d times (Accuracy: %s).%n", expectedCountFiltered, allFiltered, percentage(expectedCountFiltered, allFiltered));

        System.out.println("++++++++++++++++++++++++++++++++++++++");
        System.out.println("++++++++++++++++++++++++++++++++++++++");
    }

    private static void printTechnicalSuccess(final List<PatchOutcome> allOutcomes) {
        final long commitPatches = allOutcomes.size();
        final long commitSuccessNormal = allOutcomes.stream().filter(o -> o.lineSuccessNormal() == o.lineNormal()).count();
        System.out.printf("%d of %d commit-sized patch applications succeeded (%s)%n", commitSuccessNormal, commitPatches, percentage(commitSuccessNormal, commitPatches));

        final long fileNormal = allOutcomes.stream().mapToLong(PatchOutcome::fileNormal).sum();
        final long fileSuccessNormal = allOutcomes.stream().mapToLong(PatchOutcome::fileSuccessNormal).sum();

        System.out.printf("%d of %d file-sized patch applications succeeded (%s)%n", fileSuccessNormal, fileNormal, percentage(fileSuccessNormal, fileNormal));

        final long lineNormal = allOutcomes.stream().mapToLong(PatchOutcome::lineNormal).sum();
        final long lineSuccessNormal = allOutcomes.stream().mapToLong(PatchOutcome::lineSuccessNormal).sum();
        System.out.printf("%d of %d line-sized patch applications succeeded (%s)%n", lineSuccessNormal, lineNormal, percentage(lineSuccessNormal, lineNormal));

        // -------------------
        final long commitSuccessFiltered = allOutcomes.stream().filter(o -> o.lineSuccessFiltered() == o.lineFiltered()).count();
        System.out.printf("%d of %d filtered commit-sized patch applications succeeded (%s)%n", commitSuccessFiltered, commitPatches, percentage(commitSuccessFiltered, commitPatches));

        final long fileFiltered = allOutcomes.stream().mapToLong(PatchOutcome::fileFiltered).sum();
        final long fileSuccessFiltered = allOutcomes.stream().mapToLong(PatchOutcome::fileSuccessFiltered).sum();
        System.out.printf("%d of %d filtered file-sized patch applications succeeded (%s)%n", fileSuccessFiltered, fileFiltered, percentage(fileSuccessFiltered, fileFiltered));

        final long lineFiltered = allOutcomes.stream().mapToLong(PatchOutcome::lineFiltered).sum();
        final long lineSuccessFiltered = allOutcomes.stream().mapToLong(PatchOutcome::lineSuccessFiltered).sum();
        System.out.printf("%d of %d filtered line-sized patch applications succeeded (%s)%n", lineSuccessFiltered, lineFiltered, percentage(lineSuccessFiltered, lineFiltered));

    }

    private static void printPrecisionRecall(final long tp, final long fp, final long tn, final long fn, final long wrongLocation) {
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
        System.out.printf("%d of %d false negatives are due to the patch being applied in the wrong place (%s).",wrongLocation, fn, percentage(wrongLocation, fn));
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

    private static record ConditionTable(List<Change> tp, List<Change> fp, List<Change> tn, List<Change> fn,
                                         List<Change> wrongLocation) {
        public long tpCount() {
            return tp.size();
        }

        public long fpCount() {
            return fp.size();
        }

        public long tnCount() {
            return tn.size();
        }

        public long fnCount() {
            return fn.size();
        }

        /**
         * @return Number of false negatives applied in the wrong location
         */
        public long wrongLocationCount() {
            return wrongLocation.size();
        }
    }
}
