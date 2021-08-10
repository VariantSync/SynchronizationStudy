package de.variantsync.studies.sync.experiment;

import de.variantsync.evolution.util.Logger;
import de.variantsync.evolution.variability.SPLCommit;
import de.variantsync.studies.sync.diff.DiffParser;
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
    /*
    can't find file to patch at input line 4
Perhaps you used the wrong -p or --strip option?
The text leading up to this was:
--------------------------
|diff -N -a -u -r V0Variants/Variant0/editors/awk.c V1Variants/Variant0/editors/awk.c
|--- V0Variants/Variant0/editors/awk.c	2021-08-10 13:05:03.081006838 +0000
|+++ V1Variants/Variant0/editors/awk.c	2021-08-10 13:05:03.131006835 +0000
--------------------------
No file to patch.  Skipping patch.
1 out of 1 hunk ignored
can't find file to patch at input line 15
Perhaps you used the wrong -p or --strip option?
The text leading up to this was:
--------------------------
|diff -N -a -u -r V0Variants/Variant0/editors/awk.c V1Variants/Variant0/editors/awk.c
|--- V0Variants/Variant0/editors/awk.c	2021-08-10 13:05:03.081006838 +0000
|+++ V1Variants/Variant0/editors/awk.c	2021-08-10 13:05:03.131006835 +0000
--------------------------
No file to patch.  Skipping patch.
1 out of 1 hunk ignored
can't find file to patch at input line 26
Perhaps you used the wrong -p or --strip option?
The text leading up to this was:
--------------------------
|diff -N -a -u -r V0Variants/Variant0/editors/awk.c V1Variants/Variant0/editors/awk.c
|--- V0Variants/Variant0/editors/awk.c	2021-08-10 13:05:03.081006838 +0000
|+++ V1Variants/Variant0/editors/awk.c	2021-08-10 13:05:03.131006835 +0000
--------------------------
No file to patch.  Skipping patch.
1 out of 1 hunk ignored
*/
    
    public static void analyze(SPLCommit commitV0, SPLCommit commitV1, 
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

        System.out.println("STOP");
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
}
