import sys
import os

import parse
import plot
import serialization

if __name__ == "__main__":
    path = sys.argv[1]
    cachedFile = path + ".cache"
    outputDirectory = "out"

    if os.path.exists(cachedFile):
        print("Loading chache", cachedFile)
        experiment = serialization.deserialize(cachedFile)
    else:
        print("No chache found at", cachedFile)
        print("Opening", path)
        experiment = parse.parseFileAt(path)
        serialization.serialize(experiment, cachedFile)

    print()
    print("Parsed Values:")
    print("commitPatches =", experiment.normal.commitPatches)
    print("normal =", vars(experiment.normal))
    print("filtered =", vars(experiment.filtered))
    print()

    plot.rq1(experiment.normal, outputDirectory)
    # plot.rq2(experiment.normal, outputDirectory)
    # plot.rq3(experiment.filtered, outputDirectory)

    print("Done")
