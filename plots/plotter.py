import sys
import parse

if __name__ == "__main__":
    path = sys.argv[1]
    print("Opening", path)

    experiment = parse.parseFileAt(path)
    
    print()
    print("Parsed Values:")
    print("commitPatches =", experiment.normal.commitPatches)
    print("normal =", vars(experiment.normal))
    print("filtered =", vars(experiment.filtered))
    print()

    print("Done")