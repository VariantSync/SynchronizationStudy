package de.variantsync.studies.sync.diff.components;

import java.util.List;

public interface IDiffComponent {
    /**
     * Parse this component back into a list of lines that can be added to a diff file meant as input for unix patch
     *
     * @return the lines of the part of the diff that this component represents
     */
    List<String> toLines();
}