import sys
import json
import re

REGEX_JSON_FIELD = "^(.*)\s*:\s*([^,]*)\s*(,)?\s*$"
REGEX_PATTERN_JSON_FIELD = re.compile(REGEX_JSON_FIELD)

class Experiment:
    tp = 0
    fp = 0
    tn = 0
    fn = 0

    wrongLocation = 0

    commitSuccess = 0

    file = 0
    fileSuccess = 0

    line = 0
    lineSuccess = 0

commitPatches = 0
normal = Experiment()
filtered = Experiment()

def consume(json_object):
    global commitPatches
    global normal
    global filtered

    outcome = json.loads(json_object)

    normal.tp += outcome['normalTP']
    normal.fp += outcome['normalFP']
    normal.tn += outcome['normalTN']
    normal.fn += outcome['normalFN']

    filtered.tp += outcome['filteredTP']
    filtered.fp += outcome['filteredFP']
    filtered.tn += outcome['filteredTN']
    filtered.fn += outcome['filteredFN']

    normal.wrongLocation += outcome['normalWrongLocation']
    filtered.wrongLocation += outcome['filteredWrongLocation']

    commitPatches = commitPatches + 1
    if outcome['lineSuccessNormal'] == outcome['lineNormal']:
        normal.commitSuccess = normal.commitSuccess + 1
    if outcome['lineSuccessFiltered'] == outcome['lineFiltered']:
        filtered.commitSuccess = filtered.commitSuccess + 1

    normal.file += outcome['fileNormal']
    normal.fileSuccess += outcome['fileSuccessNormal']
    filtered.file += outcome['fileFiltered']
    filtered.fileSuccess += outcome['fileSuccessFiltered']

    normal.line += outcome['lineNormal']
    normal.lineSuccess += outcome['lineSuccessNormal']
    filtered.line += outcome['lineFiltered']
    filtered.lineSuccess += outcome['lineSuccessFiltered']


if __name__ == "__main__":
    path = sys.argv[1]
    print("Opening", path)
    with open(path) as file:
        json_object = ""
        # This reads lines lazily according to https://stackoverflow.com/questions/519633/lazy-method-for-reading-big-file-in-python
        for line in file:
            stripped = line.strip()
            # print("PARSE", stripped)
            if len(stripped) == 0:
                # print("CONSUME")
                consume(json_object)
                json_object = ""
            else:
                # print("ADD")
                if stripped != "{" and stripped != "}":
                    match = REGEX_PATTERN_JSON_FIELD.match(stripped)
                    key = match.group(1)
                    val = match.group(2)
                    comma = match.group(3)
                    if not val.isdigit():
                        stripped = key + ": \"" + val + "\""
                        if comma != None: # If there is a comma
                            stripped += ","
                stripped += "\n"

                json_object += stripped

        if len(json_object) > 0:
            consume(json_object)
    
    print()
    print("Parsed Values:")
    print("commitPatches =", commitPatches)
    print("normal =", vars(normal))
    print("filtered =", vars(filtered))
    print()

    print("Done")