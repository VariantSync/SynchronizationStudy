package de.variantsync.studies.sync.experiment;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.variantsync.evolution.util.Logger;
import de.variantsync.evolution.variability.SPLCommit;
import de.variantsync.studies.sync.diff.components.FineDiff;
import de.variantsync.studies.sync.diff.components.OriginalDiff;

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
                                              final OriginalDiff actualVsExpectedNormal, final OriginalDiff actualVsExpectedFiltered,
                                              final OriginalDiff rejectsNormal, final OriginalDiff rejectsFiltered,
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

        // false negatives = relevant that failed
        final int normalFN = lineFilteredFailed;
        // true positives = relevant - false negatives
        final int normalTP = lineFiltered - normalFN;
        // false positive = selected - true positive
        final int normalFP = selected - normalTP;
        // true negative = all - relevant - false positive
        final int normalTN = lineNormal - lineFiltered - normalFP;

        final int filteredTP = lineFiltered - lineFilteredFailed;
        final int filteredFP = 0;
        final int filteredTN = lineNormal - lineFiltered;
        final int filteredFN = lineFilteredFailed;

        assert normalTP + normalFP + normalFN + normalTN == filteredTP + filteredFP + filteredTN + filteredFN;
        assert filteredTP + filteredFN == lineFiltered;
        assert normalTP + normalFN == lineFiltered;
        assert filteredFP + filteredTN == lineNormal - lineFiltered;
        assert normalFP + normalTN == lineNormal - lineFiltered;
        assert normalTP <= filteredTP;
        assert normalTN <= filteredTN;

        return new PatchOutcome(dataset,
                runID,
                commitV0.id(),
                commitV1.id(),
                sourceVariant,
                targetVariant,
                actualVsExpectedNormal.isEmpty(),
                actualVsExpectedFiltered.isEmpty(),
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

        final long countNormal = allOutcomes.stream().filter(PatchOutcome::normalAsExpected).count();
        final long countFiltered = allOutcomes.stream().filter(PatchOutcome::filteredAsExpected).count();
        System.out.printf("Normal patching achieved the expected result %d out of %d times.%n", countNormal, allOutcomes.size());
        System.out.printf("Filtered patching achieved the expected result %d out of %d times.%n", countFiltered, allOutcomes.size());
        
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
