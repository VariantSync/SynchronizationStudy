package de.variantsync.studies.sync.diff;

import java.util.function.Function;

public interface ILineFilter extends Function<Line, Boolean> {
    @Override
    Boolean apply(Line line);
}