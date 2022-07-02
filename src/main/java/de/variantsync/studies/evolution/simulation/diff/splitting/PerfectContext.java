package de.variantsync.studies.evolution.simulation.diff.splitting;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record PerfectContext(List<String> lines, Map<Integer, Integer> indexMap, Path sourceFile) {

}
