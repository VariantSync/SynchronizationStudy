```java
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
```