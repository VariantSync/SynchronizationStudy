package de.variantsync.studies.sync.experiment;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.variantsync.evolution.util.Logger;
import de.variantsync.evolution.variability.SPLCommit;
import de.variantsync.studies.sync.diff.components.FineDiff;
import de.variantsync.studies.sync.diff.components.Hunk;
import de.variantsync.studies.sync.diff.components.OriginalDiff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ResultAnalysis {
    static Path resultPath = Path.of("/home/alex/data/synchronization-study/workdir/results.txt");

    public static PatchOutcome processOutcome(String dataset,
                                              long runID,
                                              String sourceVariant,
                                              String targetVariant,
                                              SPLCommit commitV0, SPLCommit commitV1,
                                              FineDiff normalPatch, FineDiff filteredPatch,
                                              OriginalDiff actualVsExpectedNormal, OriginalDiff actualVsExpectedFiltered,
                                              OriginalDiff rejectsNormal, OriginalDiff rejectsFiltered,
                                              List<Path> skippedFilesNormal) {
        // evaluate patch rejects
        int fileNormal = new HashSet<>(normalPatch.content().stream().map(fd -> fd.oldFile().toString()).collect(Collectors.toList())).size();
        int lineNormal = normalPatch.content().stream().mapToInt(fd -> fd.hunks().size()).sum();
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

        int fileFiltered = new HashSet<>(filteredPatch.content().stream().map(fd -> fd.oldFile().toString()).collect(Collectors.toList())).size();
        int lineFiltered = filteredPatch.content().stream().mapToInt(fd -> fd.hunks().size()).sum();
        int fileFilteredFailed = 0;
        int lineFilteredFailed = 0;
        if (rejectsFiltered != null) {
            fileFilteredFailed = new HashSet<>(rejectsFiltered.fileDiffs().stream().map(fd -> fd.oldFile().toString()).collect(Collectors.toList())).size();
            Logger.status("" + fileFilteredFailed + " of " + fileFiltered + " filtered file-sized patches failed.");
            lineFilteredFailed = rejectsFiltered.fileDiffs().stream().mapToInt(fd -> fd.hunks().size()).sum();
            Logger.status("" + lineFilteredFailed + " of " + lineFiltered + " filtered line-sized patches failed");
        }

        Set<Hunk> allPatches = toHunks(normalPatch);
        Set<Hunk> relevantPatches = toHunks(filteredPatch);
        Set<Hunk> failedNormalPatches = toHunks(rejectsNormal);
        Set<Hunk> failedFilteredPatches = toHunks(rejectsFiltered);
        Set<Hunk> skippedNormalPatches = toHunks(normalPatch, skippedFilesNormal);
        Set<Hunk> skippedFilteredPatches = new HashSet<>(allPatches);
        skippedFilteredPatches.removeAll(relevantPatches);

        Set<Hunk> successfulNormalPatches = new HashSet<>(allPatches);
        successfulNormalPatches.removeAll(failedNormalPatches);
        successfulNormalPatches.removeAll(skippedNormalPatches);

        Set<Hunk> successfulFilteredPatches = new HashSet<>(relevantPatches);
        successfulFilteredPatches.removeAll(failedFilteredPatches);

        int filteredTP = successfulFilteredPatches.size();
        int filteredFP = 0;
        int filteredTN = skippedFilteredPatches.size();
        int filteredFN = failedFilteredPatches.size();

        int normalTP = (int) successfulNormalPatches.stream().filter(relevantPatches::contains).count();
        int normalFP = (int) successfulNormalPatches.stream().filter(p -> !relevantPatches.contains(p)).count();
        int normalTN = (int) skippedNormalPatches.stream().filter(p -> !relevantPatches.contains(p)).count();
        normalTN += failedNormalPatches.stream().filter(p -> !relevantPatches.contains(p)).count();
        int normalFN = (int) failedNormalPatches.stream().filter(relevantPatches::contains).count();

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

    private static Set<Hunk> toHunks(FineDiff diff) {
        if (diff == null) {
            return new HashSet<>();
        }
        return diff.content().stream().flatMap(fd -> fd.hunks().stream()).collect(Collectors.toSet());
    }

    private static Set<Hunk> toHunks(FineDiff diff, List<Path> skippedFiles) {
        if (diff == null) {
            return new HashSet<>();
        }
        return diff.content().stream().filter(fd -> skippedFiles.contains(fd.oldFile())).flatMap(fd -> fd.hunks().stream()).collect(Collectors.toSet());
    }

    private static Set<Hunk> toHunks(OriginalDiff diff) {
        if (diff == null) {
            return new HashSet<>();
        }
        return diff.fileDiffs().stream().flatMap(fd -> fd.hunks().stream()).collect(Collectors.toSet());
    }

    public static void main(String... args) throws IOException {
        List<PatchOutcome> allOutcomes = loadResultObjects(resultPath);
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

        System.out.println("++++++++++++++++++++++++++++++++++++++");
        System.out.println("++++++++++++++++++++++++++++++++++++++");

    }

    private static void printTechnicalSuccess(List<PatchOutcome> allOutcomes) {
        long commitPatches = allOutcomes.size();
        long commitSuccess = allOutcomes.stream().filter(o -> o.lineSuccessNormal() == o.lineNormal()).count();
        System.out.printf("%d of %d commit-sized patch applications succeeded (%s)%n", commitSuccess, commitPatches, percentage(commitSuccess, commitPatches));

        long fileNormal = allOutcomes.stream().mapToLong(PatchOutcome::fileNormal).sum();
        long fileSuccessNormal = allOutcomes.stream().mapToLong(PatchOutcome::fileSuccessNormal).sum();
        
        System.out.printf("%d of %d file-sized patch applications succeeded (%s)%n", fileSuccessNormal, fileNormal, percentage(fileSuccessNormal, fileNormal));

        long lineNormal = allOutcomes.stream().mapToLong(PatchOutcome::lineNormal).sum();
        long lineSuccessNormal = allOutcomes.stream().mapToLong(PatchOutcome::lineSuccessNormal).sum();
        System.out.printf("%d of %d line-sized patch applications succeeded (%s)%n", lineSuccessNormal, lineNormal, percentage(lineSuccessNormal, lineNormal));

        long fileFiltered = allOutcomes.stream().mapToLong(PatchOutcome::fileFiltered).sum();
        long fileSuccessFiltered = allOutcomes.stream().mapToLong(PatchOutcome::fileSuccessFiltered).sum();
        System.out.printf("%d of %d filtered file-sized patch applications succeeded (%s)%n", fileSuccessFiltered, fileFiltered, percentage(fileSuccessFiltered, fileFiltered));

        long lineFiltered = allOutcomes.stream().mapToLong(PatchOutcome::lineFiltered).sum();
        long lineSuccessFiltered = allOutcomes.stream().mapToLong(PatchOutcome::lineSuccessFiltered).sum();
        System.out.printf("%d of %d filtered line-sized patch applications succeeded (%s)%n", lineSuccessFiltered, lineFiltered, percentage(lineSuccessFiltered, lineFiltered));

    }

    private static void printPrecisionRecall(long tp, long fp, long tn, long fn) {
        double precision = (double) tp / ((double) tp + fp);
        double recall = (double) tp / ((double) tp + fn);
        double f_measure = (2 * precision * recall) / (precision + recall);

        System.out.println("TP: " + tp);
        System.out.println("FP: " + fp);
        System.out.println("TN: " + tn);
        System.out.println("FN: " + fn);
        System.out.printf("Precision: %1.2f%n", precision);
        System.out.printf("Recall: %1.2f%n", recall);
        System.out.printf("F-Measure: %1.2f%n", f_measure);
    }

    public static List<PatchOutcome> loadResultObjects(Path path) throws IOException {
        List<PatchOutcome> results = new LinkedList<>();
        List<String> lines = Files.readAllLines(path);
        List<String> currentResult = new LinkedList<>();
        for (String l : lines) {
            if (l.isEmpty()) {
                results.add(parseResult(currentResult));
                currentResult = new LinkedList<>();
            } else {
                currentResult.add(l);
            }
        }
        return results;
    }

    public static PatchOutcome parseResult(List<String> lines) {
        Gson gson = new Gson();
        StringBuilder sb = new StringBuilder();
        lines.forEach(l -> sb.append(l).append("\n"));
        JsonObject object = gson.fromJson(sb.toString(), JsonObject.class);
        return PatchOutcome.FromJSON(object);
    }

    public static String percentage(long x, long y) {
        double percentage;
        if (y == 0) {
            percentage = 0;
        } else {
            percentage = 100 * ((double) x / (double) y);
        }
        return String.format("%3.1f%s", percentage, "%");
    }
}
