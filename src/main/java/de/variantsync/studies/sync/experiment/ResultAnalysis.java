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

import java.io.BufferedReader;
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
            fileFilteredFailed += filteredPatch.content().stream().filter(fd -> skippedFilesFiltered.contains(fd.oldFile())).count();
            Logger.status("" + fileFilteredFailed + " of " + fileFiltered + " filtered file-sized patches failed.");
            lineFilteredFailed = rejectsFiltered.fileDiffs().stream().mapToInt(fd -> fd.hunks().size()).sum();
            lineFilteredFailed += filteredPatch.content().stream().filter(fd -> skippedFilesFiltered.contains(fd.oldFile())).mapToInt(fd -> fd.hunks().size()).sum();
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
        final List<Change> unpatchableChanges = new LinkedList<>();
        // Determine expected changes, i.e., changes in the target variant's evolution that can be synchronized
        final List<Change> expectedChanges = new LinkedList<>();
        {
            final List<Change> tempChanges = new LinkedList<>(changesInUnfilteredPatch);
            for (Change evolutionChange : changesInEvolution) {
                if (!tempChanges.contains(evolutionChange)) {
                    unpatchableChanges.add(evolutionChange);
                } else {
                    expectedChanges.add(evolutionChange);
                    tempChanges.remove(evolutionChange);
                }
            }
        }

        // Determine actual differences between result and expected result,
        // i.e., changes that should have been synchronized but were not, or changes that should not have been synchronized
        final List<Change> actualDifferences = new LinkedList<>(changesInResult);
        unpatchableChanges.forEach(actualDifferences::remove);

        assert changesInUnfilteredPatch.size() >= changesInPatch.size();
        assert changesInEvolution.size() - changesInUnfilteredPatch.size() <= unpatchableChanges.size();
        assert changesInEvolution.size() - unpatchableChanges.size() <= changesInUnfilteredPatch.size();
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
                changesInPatch.remove(actualDifference);
                changesInUnfilteredPatch.remove(actualDifference);
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
            Change oppositeChange;
            if (remainingDifference.line() instanceof AddedLine) {
                oppositeChange = new Change(remainingDifference.file(), new RemovedLine("-" + changedText));
            } else {
                oppositeChange = new Change(remainingDifference.file(), new AddedLine("+" + changedText));
            }
            if (changesInPatch.contains(oppositeChange)) {
                // The patch contained a false positive, as the difference was not expected.
                changesInPatch.remove(oppositeChange);
                changesInUnfilteredPatch.remove(oppositeChange);
                fpChanges.add(oppositeChange);
            } else {
                // This case happens if a line has been synchronized, but to the wrong location. This will result
                // in the actual differences containing the line once to be added and once to be removed. Therefore,
                // it's opposite change has already been removed from the changes in the patch.
                wrongLocation.add(remainingDifference);
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
        final AccumulatedOutcome allOutcomes = loadResultObjects(resultPath);
        System.out.println();
        System.out.println("++++++++++++++++++++++++++++++++++++++");
        System.out.println("Patch Success");
        System.out.println("++++++++++++++++++++++++++++++++++++++");
        printTechnicalSuccess(allOutcomes);

        System.out.println();
        System.out.println("++++++++++++++++++++++++++++++++++++++");
        System.out.println("Precision / Recall");
        System.out.println("++++++++++++++++++++++++++++++++++++++");

        long normalTP = allOutcomes.normalTP;
        long normalFP = allOutcomes.normalFP;
        long normalTN = allOutcomes.normalTN;
        long normalFN = allOutcomes.normalFN;

        printPrecisionRecall(normalTP,
                normalFP,
                normalTN,
                normalFN,
                allOutcomes.normalWrongLocation);


        System.out.println();
        System.out.println("++++++++++++++++++++++++++++++++++++++");
        System.out.println();

        long filteredTP = allOutcomes.filteredTP;
        long filteredFP = allOutcomes.filteredFP;
        long filteredTN = allOutcomes.filteredTN;
        long filteredFN = allOutcomes.filteredFN;

        printPrecisionRecall(filteredTP,
                filteredFP,
                filteredTN,
                filteredFN,
                allOutcomes.filteredWrongLocation);

        System.out.println("++++++++++++++++++++++++++++++++++++++");
        System.out.println("Accuracy");
        System.out.println("++++++++++++++++++++++++++++++++++++++");

       printAccuracy(normalTP, normalFP, normalTN, normalFN, "Normal");
       printAccuracy(filteredTP, filteredFP, filteredTN, filteredFN, "Filtered");

        System.out.println("++++++++++++++++++++++++++++++++++++++");
        System.out.println("++++++++++++++++++++++++++++++++++++++");
    }

    private static void printAccuracy(long tp, long fp, long tn, long fn, String name) {
        long expectedCount = tp + tn;
        long allPositives = tp + fn;
        long allNegative = fp + tn;
        double truePositiveRate = (double)tp / (double)allPositives;
        double trueNegativeRate = (double)tn / (double)allNegative;
        long all = tp + fp + tn + fn;

        System.out.printf("%s patching achieved the expected result %d out of %d times%n", name, expectedCount, all);
        System.out.printf("Accuracy: %s%n", percentage(expectedCount, all));
        System.out.printf("Balanced Accuracy: %1.2f%n%n", ((truePositiveRate + trueNegativeRate) / 2.0));
    }

    private static void printTechnicalSuccess(final AccumulatedOutcome allOutcomes) {
        final long commitPatches = allOutcomes.commitPatches();
        final long commitSuccessNormal = allOutcomes.commitSuccessNormal();
        System.out.printf("%d of %d commit-sized patch applications succeeded (%s)%n", commitSuccessNormal, commitPatches, percentage(commitSuccessNormal, commitPatches));

        final long fileNormal = allOutcomes.fileNormal;
        final long fileSuccessNormal = allOutcomes.fileSuccessNormal;

        System.out.printf("%d of %d file-sized patch applications succeeded (%s)%n", fileSuccessNormal, fileNormal, percentage(fileSuccessNormal, fileNormal));

        final long lineNormal = allOutcomes.lineNormal;
        final long lineSuccessNormal = allOutcomes.lineSuccessNormal;
        System.out.printf("%d of %d line-sized patch applications succeeded (%s)%n", lineSuccessNormal, lineNormal, percentage(lineSuccessNormal, lineNormal));

        // -------------------
        final long commitSuccessFiltered = allOutcomes.commitSuccessFiltered;
        System.out.printf("%d of %d filtered commit-sized patch applications succeeded (%s)%n", commitSuccessFiltered, commitPatches, percentage(commitSuccessFiltered, commitPatches));

        final long fileFiltered = allOutcomes.fileFiltered;
        final long fileSuccessFiltered = allOutcomes.fileSuccessFiltered;
        System.out.printf("%d of %d filtered file-sized patch applications succeeded (%s)%n", fileSuccessFiltered, fileFiltered, percentage(fileSuccessFiltered, fileFiltered));

        final long lineFiltered = allOutcomes.lineFiltered;
        final long lineSuccessFiltered = allOutcomes.lineSuccessFiltered;
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
        System.out.printf("-------------%nApplied to wrong location: %d%nPercentage of FN: %s%nPercentage total: %s%n", wrongLocation, percentage(wrongLocation, fn), percentage(wrongLocation, tp + fp + tn + fn));
    }

    public static AccumulatedOutcome loadResultObjects(final Path path) throws IOException {
        long normalTP = 0;
        long normalFP = 0;
        long normalTN = 0;
        long normalFN = 0;

        long filteredTP = 0;
        long filteredFP = 0;
        long filteredTN = 0;
        long filteredFN = 0;

        long normalWrongLocation = 0;
        long filteredWrongLocation = 0;

        long commitPatches = 0;
        long commitSuccessNormal = 0;
        long commitSuccessFiltered = 0;

        long fileNormal = 0;
        long fileFiltered = 0;
        long fileSuccessNormal = 0;
        long fileSuccessFiltered = 0;

        long lineNormal = 0;
        long lineFiltered = 0;
        long lineSuccessNormal = 0;
        long lineSuccessFiltered = 0;

        try(BufferedReader reader = Files.newBufferedReader(path)) {
            List<String> outcomeLines = new LinkedList<>();
            for(String line = reader.readLine(); line != null; line= reader.readLine()) {
                if (line.isEmpty()) {
                    PatchOutcome outcome = parseResult(outcomeLines);
                    normalTP += outcome.normalTP();
                    normalFP += outcome.normalFP();
                    normalTN += outcome.normalTN();
                    normalFN += outcome.normalFN();

                    filteredTP += outcome.filteredTP();
                    filteredFP += outcome.filteredFP();
                    filteredTN += outcome.filteredTN();
                    filteredFN += outcome.filteredFN();

                    normalWrongLocation += outcome.normalWrongLocation();
                    filteredWrongLocation += outcome.filteredWrongLocation();

                    commitPatches++;
                    if (outcome.lineSuccessNormal() == outcome.lineNormal()) {
                        commitSuccessNormal++;
                    }
                    if (outcome.lineSuccessFiltered() == outcome.lineFiltered()) {
                        commitSuccessFiltered++;
                    }

                    fileNormal += outcome.fileNormal();
                    fileSuccessNormal += outcome.fileSuccessNormal();
                    fileFiltered += outcome.fileFiltered();
                    fileSuccessFiltered += outcome.fileSuccessFiltered();

                    lineNormal += outcome.lineNormal();
                    lineSuccessNormal += outcome.lineSuccessNormal();
                    lineFiltered += outcome.lineFiltered();
                    lineSuccessFiltered += outcome.lineSuccessFiltered();

                    outcomeLines.clear();
                } else {
                    outcomeLines.add(line);
                }
            }
        }

        System.out.printf("Read a total of %d results.", commitPatches);

        return new AccumulatedOutcome(
                normalTP, normalFP, normalTN, normalFN,
                filteredTP, filteredFP, filteredTN, filteredFN,
                normalWrongLocation, filteredWrongLocation,
                commitPatches, commitSuccessNormal, commitSuccessFiltered,
                fileNormal, fileFiltered, fileSuccessNormal, fileSuccessFiltered,
                lineNormal, lineFiltered, lineSuccessNormal, lineSuccessFiltered
        );
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

    private static record AccumulatedOutcome(
            long normalTP,
            long normalFP,
            long normalTN,
            long normalFN,
            long filteredTP,
            long filteredFP,
            long filteredTN,
            long filteredFN,
            long normalWrongLocation,
            long filteredWrongLocation,
            long commitPatches,
            long commitSuccessNormal,
            long commitSuccessFiltered,
            long fileNormal,
            long fileFiltered,
            long fileSuccessNormal,
            long fileSuccessFiltered,
            long lineNormal,
            long lineFiltered,
            long lineSuccessNormal,
            long lineSuccessFiltered
    ){}
}
