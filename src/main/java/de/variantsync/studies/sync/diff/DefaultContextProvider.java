package de.variantsync.studies.sync.diff;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class DefaultContextProvider implements IContextProvider {
    private final int contextSize;

    public DefaultContextProvider() {
        // Three is the default size set in unix diff
        this(3);
    }

    public DefaultContextProvider(int contextSize) {
        this.contextSize = contextSize;
    }

    @Override
    public List<Line> leadingContext(ILineFilter lineFilter, FileDiff fileDiff, Hunk hunk, int lineNumber) {
        List<Line> lines = new LinkedList<>();
        for (int i = lineNumber - 1; i >= 0; i--) {
            Line currentLine = hunk.content().get(i);
            if (lineFilter.shouldKeep(fileDiff, hunk, i)) {
                if (currentLine instanceof MetaLine metaLine) {
                    lines.add(metaLine);
                } else {
                    if (lines.size() >= contextSize) {
                        break;
                    }
                    if (currentLine instanceof ContextLine contextLine) {
                        lines.add(contextLine);
                    } else if (currentLine instanceof AddedLine addedLine) {
                        lines.add(new ContextLine(addedLine));
                    }
                }
            }
        }
        Collections.reverse(lines);
        return lines;
    }

    @Override
    public List<Line> trailingContext(ILineFilter lineFilter, FileDiff fileDiff, Hunk hunk, int lineNumber) {
        List<Line> lines = new LinkedList<>();
        for (int i = lineNumber + 1; i < hunk.content().size(); i++) {
            Line currentLine = hunk.content().get(i);
            if (lineFilter.shouldKeep(fileDiff, hunk, i)) {
                if (currentLine instanceof MetaLine metaLine) {
                    lines.add(metaLine);
                } else {
                    if (lines.size() >= contextSize) {
                        break;
                    }
                    if (currentLine instanceof ContextLine contextLine) {
                        lines.add(contextLine);
                    } else if (currentLine instanceof RemovedLine removedLine) {
                        lines.add(new ContextLine(removedLine));
                    }
                }
            }
        }
        return lines;
    }
}