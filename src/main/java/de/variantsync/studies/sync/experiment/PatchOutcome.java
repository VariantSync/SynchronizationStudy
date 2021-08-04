package de.variantsync.studies.sync.experiment;

import de.variantsync.studies.sync.diff.components.FineDiff;
import de.variantsync.studies.sync.diff.components.OriginalDiff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;

public record PatchOutcome(String dataset,
                           long runID,
                           EPatchType patchType,
                           CommitIDV0 commitV0,
                           CommitIDV1 commitV1,
                           SourceVariant sourceVariant,
                           TargetVariant targetVariant,
                           AppliedPatch appliedPatch,
                           ActualVsExpectedTargetV1 actualVsExpected,
                           PatchRejects rejects,
                           FileSizedEditCount fileSizedEditCount,
                           LineSizedEditCount lineSizedEditCount,
                           FailedFileSizedEditCount failedFileSizedEditCount,
                           FailedLineSizedEditCount failedLineSizedEditCount) {

    public static String toJSON(JSONObject object, String name) {
        return object == null ? "\"" + name + "\": null" : object.toJSON(name);
    }

    public static String collectionToJSON(Collection<String> collection) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        collection.forEach(l -> sb.append("\"").append(l).append("\"").append(","));
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

    public void writeAsJSON(Path pathToFile, boolean append) throws IOException {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{").append("\n");
        jsonBuilder.append("\"ID\": \"").append(runID).append("\"").append(",\n");
        jsonBuilder.append("\"Dataset\": \"").append(dataset).append("\"").append(",\n");
        jsonBuilder.append("\"PatchType\": \"").append(patchType).append("\"").append(",\n");
        jsonBuilder.append(toJSON(commitV0, "CommitID-V0")).append(",\n");
        jsonBuilder.append(toJSON(commitV1, "CommitID-V1")).append(",\n");
        jsonBuilder.append(toJSON(sourceVariant, "sourceVariant")).append(",\n");
        jsonBuilder.append(toJSON(targetVariant, "targetVariant")).append(",\n");
//        jsonBuilder.append(toJSON(appliedPatch, "appliedPatch")).append(",\n");
        jsonBuilder.append(toJSON(actualVsExpected, "actualVsExpected")).append(",\n");
        jsonBuilder.append(toJSON(rejects, "rejects")).append(",\n");
        jsonBuilder.append("\"CommitPatches\": \"1\"").append(",\n");
        jsonBuilder.append("\"FailedCommitPatches\": \"").append(failedLineSizedEditCount.count > 0 ? 1 : 0).append("\"").append(",\n");
        jsonBuilder.append(toJSON(fileSizedEditCount, "filePatches")).append(",\n");
        jsonBuilder.append(toJSON(failedFileSizedEditCount, "failedFilePatches")).append(",\n");
        jsonBuilder.append(toJSON(new Percentage(failedFileSizedEditCount.count, fileSizedEditCount.count()), "percentageFailedFilePatches")).append(",\n");
        jsonBuilder.append(toJSON(lineSizedEditCount, "linePatches")).append(",\n");
        jsonBuilder.append(toJSON(failedLineSizedEditCount, "failedLinePatches")).append("\n");
        jsonBuilder.append(toJSON(new Percentage(failedLineSizedEditCount.count, lineSizedEditCount.count()), "percentageFailedLinePatches")).append(",\n");
        jsonBuilder.append("}").append("\n\n");
        if (!Files.exists(pathToFile)) {
            Files.createFile(pathToFile);
        }
        if (append) {
            Files.writeString(pathToFile, jsonBuilder.toString(), StandardOpenOption.APPEND);
        } else {
            Files.writeString(pathToFile, jsonBuilder.toString(), StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private interface JSONObject {
        default String toJSON() {
            return "\"" + this + "\"";
        }

        default String toJSON(String name) {
            return "\"" + name + "\": " + toJSON();
        }
    }

    public record CommitIDV0(String id) implements JSONObject {
        @Override
        public String toString() {
            return id;
        }
    }

    public record CommitIDV1(String id) implements JSONObject {
        @Override
        public String toString() {
            return id;
        }
    }

    public record SourceVariant(String name) implements JSONObject {
        @Override
        public String toString() {
            return name;
        }
    }

    public record TargetVariant(String name) implements JSONObject {
        @Override
        public String toString() {
            return name;
        }
    }

    public record AppliedPatch(FineDiff patch) implements JSONObject {
        @Override
        public String toString() {
            return collectionToJSON(patch.toLines());
        }
    }

    public record ActualVsExpectedTargetV1(OriginalDiff diff) implements JSONObject {
        @Override
        public String toString() {
            return String.valueOf(diff.isEmpty());
//            return collectionToJSON(diff.toLines());
        }
    }

    public record PatchRejects(OriginalDiff rejects) implements JSONObject {
        @Override
        public String toString() {
            return collectionToJSON(rejects.toLines());
        }
    }

    public record FileSizedEditCount(int count) implements JSONObject {
        @Override
        public String toString() {
            return String.valueOf(count);
        }
    }

    public record LineSizedEditCount(int count) implements JSONObject {
        @Override
        public String toString() {
            return String.valueOf(count);
        }
    }

    public record FailedFileSizedEditCount(int count) implements JSONObject {
        @Override
        public String toString() {
            return String.valueOf(count);
        }
    }

    public record FailedLineSizedEditCount(int count) implements JSONObject {
        @Override
        public String toString() {
            return String.valueOf(count);
        }
    }

    public static class Percentage implements JSONObject {
        private final double percentage;

        public Percentage(int failed, int all) {
            this.percentage = 100 * ((double) failed / (double) all);
        }

        @Override
        public String toString() {
            return String.format("%3.1f%s", percentage, "%");
        }
    }

}
