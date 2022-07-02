package de.variantsync.studies.evolution.simulation.diff.splitting;

import de.variantsync.studies.evolution.simulation.diff.components.FileDiff;
import de.variantsync.studies.evolution.simulation.diff.filter.ILineFilter;
import de.variantsync.studies.evolution.simulation.diff.lines.Line;

import java.util.List;

public interface IContextProvider {
    List<Line> leadingContext(ILineFilter lineFilter, FileDiff fileDiff, int index);

    List<Line> trailingContext(ILineFilter lineFilter, FileDiff fileDiff, int index);
}