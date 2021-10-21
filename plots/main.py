import sys

import parse
import plot

if __name__ == "__main__":
    path = sys.argv[1]
    outputDirectory = "out"

    # print("Opening", path)
    experiment = parse.parseFileAt(path)
    # print()
    # print("Parsed Values:")
    # print("commitPatches =", experiment.normal.commitPatches)
    # print("normal =", vars(experiment.normal))
    # print("filtered =", vars(experiment.filtered))
    # print()

    plot.rq1(experiment.normal, outputDirectory)
    # plot.rq2(experiment.normal, outputDirectory)
    # plot.rq3(experiment.filtered, outputDirectory)

    print("Done")
