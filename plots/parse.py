import json
import re
import data

REGEX_JSON_FIELD = "^(.*)\s*:\s*([^,]*)\s*(,)?\s*$"
REGEX_PATTERN_JSON_FIELD = re.compile(REGEX_JSON_FIELD)

def parseJsonAndAddToExperiment(json_object, experiment):
    outcome = json.loads(json_object)

    experiment.normal.tp += outcome['normalTP']
    experiment.normal.fp += outcome['normalFP']
    experiment.normal.tn += outcome['normalTN']
    experiment.normal.fn += outcome['normalFN']

    experiment.filtered.tp += outcome['filteredTP']
    experiment.filtered.fp += outcome['filteredFP']
    experiment.filtered.tn += outcome['filteredTN']
    experiment.filtered.fn += outcome['filteredFN']

    experiment.normal.wrongLocation += outcome['normalWrongLocation']
    experiment.filtered.wrongLocation += outcome['filteredWrongLocation']

    experiment.normal.commitPatches = experiment.normal.commitPatches + 1
    experiment.filtered.commitPatches = experiment.filtered.commitPatches + 1
    if outcome['lineSuccessNormal'] == outcome['lineNormal']:
        experiment.normal.commitSuccess = experiment.normal.commitSuccess + 1
    if outcome['lineSuccessFiltered'] == outcome['lineFiltered']:
        experiment.filtered.commitSuccess = experiment.filtered.commitSuccess + 1

    experiment.normal.file += outcome['fileNormal']
    experiment.normal.fileSuccess += outcome['fileSuccessNormal']
    experiment.filtered.file += outcome['fileFiltered']
    experiment.filtered.fileSuccess += outcome['fileSuccessFiltered']

    experiment.normal.line += outcome['lineNormal']
    experiment.normal.lineSuccess += outcome['lineSuccessNormal']
    experiment.filtered.line += outcome['lineFiltered']
    experiment.filtered.lineSuccess += outcome['lineSuccessFiltered']

def parseFileAt(path):
    experiment = data.Experiment()

    with open(path) as file:
        json_object = ""
        # This reads lines lazily according to https://stackoverflow.com/questions/519633/lazy-method-for-reading-big-file-in-python
        for line in file:
            stripped = line.strip()
            # print("PARSE", stripped)
            if len(stripped) == 0:
                # print("CONSUME")
                parseJsonAndAddToExperiment(json_object, experiment)
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
            parseJsonAndAddToExperiment(json_object, experiment)
    
    return experiment
