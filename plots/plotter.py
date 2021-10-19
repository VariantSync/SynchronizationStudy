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
    outcome = json.loads(json_object)
    print(outcome)

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
    
    print("Done")