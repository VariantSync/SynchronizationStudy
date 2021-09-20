package de.variantsync.studies.sync.experiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigParser {
    public static void main(final String... args) throws IOException {
        final Path p = Path.of("/home/alex/data/synchronization-study/config-test.txt");
        final List<String> lines = Files.readAllLines(p);
        final List<String> activeFeatures = lines.stream().filter(l -> !l.startsWith("#")).map(l -> l.split("=")[0].trim()).filter(l -> !l.isEmpty()).collect(Collectors.toList());
        System.out.println(activeFeatures);
    }
}
