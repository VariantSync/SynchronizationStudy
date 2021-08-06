package de.variantsync.studies.sync.experiment;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.variantsync.studies.sync.diff.components.OriginalDiff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class SimpleResultAnalysis {
    
    public static void main(String... args) throws IOException {
        Path normalResultPath = Path.of("/home/alex/data/synchronization-study/workdir/results-normal.txt");
        Path pcBasedResultPath = Path.of("/home/alex/data/synchronization-study/workdir/results-pc-based.txt");
        var normalResults = loadResultObjects(normalResultPath);
        var pcBasedResults = loadResultObjects(pcBasedResultPath);
        
        int commitPatches = 0;
        int filePatches = 0;
        int linePatches = 0;
        int failedCommitPatches = 0;
        int failedFilePatches = 0;
        int failedLinePatches = 0;

        for (PatchOutcome normalResult : normalResults) {
            commitPatches++;
            filePatches += normalResult.fileSizedEditCount().count();
            linePatches += normalResult.lineSizedEditCount().count();
            failedCommitPatches += normalResult.failedFileSizedEditCount().count() > 0 ? 1 : 0;
            failedFilePatches += normalResult.failedFileSizedEditCount().count();
            failedLinePatches += normalResult.failedLineSizedEditCount().count();
        }
        
        System.out.println("Commit-sized patches: " + (commitPatches - failedCommitPatches) + " / " + commitPatches + " or " + new PatchOutcome.Percentage(commitPatches - failedCommitPatches, commitPatches) + " succeeded.");
        System.out.println("File-sized patches: " + (filePatches - failedFilePatches) + " / " + filePatches + " or " + new PatchOutcome.Percentage(filePatches - failedFilePatches, filePatches) + " succeeded.");
        System.out.println("Line-sized patches: " + (linePatches - failedLinePatches) + " / " + linePatches + " or " + new PatchOutcome.Percentage(linePatches - failedLinePatches, linePatches) + " succeeded.");

        commitPatches = 0;
        filePatches = 0;
        linePatches = 0;
        failedCommitPatches = 0;
        failedFilePatches = 0;
        failedLinePatches = 0;

        for (PatchOutcome pcBasedResult : pcBasedResults) {
            commitPatches++;
            filePatches += pcBasedResult.fileSizedEditCount().count();
            linePatches += pcBasedResult.lineSizedEditCount().count();
            failedCommitPatches += pcBasedResult.failedFileSizedEditCount().count() > 0 ? 1 : 0;
            failedFilePatches += pcBasedResult.failedFileSizedEditCount().count();
            failedLinePatches += pcBasedResult.failedLineSizedEditCount().count();
        }

        System.out.println("Commit-sized patches: " + (commitPatches - failedCommitPatches) + " / " + commitPatches + " or " + new PatchOutcome.Percentage(commitPatches - failedCommitPatches, commitPatches) + " succeeded.");
        System.out.println("File-sized patches: " + (filePatches - failedFilePatches) + " / " + filePatches + " or " + new PatchOutcome.Percentage(filePatches - failedFilePatches, filePatches) + " succeeded.");
        System.out.println("Line-sized patches: " + (linePatches - failedLinePatches) + " / " + linePatches + " or " + new PatchOutcome.Percentage(linePatches - failedLinePatches, linePatches) + " succeeded.");
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
        return new PatchOutcome(
                object.get("Dataset").getAsString(),
                object.get("ID").getAsLong(),
                EPatchType.valueOf(object.get("PatchType").getAsString()),
                new PatchOutcome.CommitIDV0(object.get("CommitID-V0").getAsString()),
                new PatchOutcome.CommitIDV1(object.get("CommitID-V1").getAsString()),
                new PatchOutcome.SourceVariant(object.get("sourceVariant").getAsString()),
                new PatchOutcome.TargetVariant(object.get("targetVariant").getAsString()),
                null,
                new PatchOutcome.ActualVsExpectedTargetV1(new OriginalDiff(new LinkedList<>())),
                null,
                new PatchOutcome.FileSizedEditCount(object.get("filePatches").getAsInt()),
                new PatchOutcome.LineSizedEditCount(object.get("linePatches").getAsInt()),
                new PatchOutcome.FailedFileSizedEditCount(object.get("failedFilePatches").getAsInt()),
                new PatchOutcome.FailedLineSizedEditCount(object.get("failedLinePatches").getAsInt())
        );
    }
}
