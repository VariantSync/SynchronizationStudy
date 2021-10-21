class PatchStrategy:
    tp = 0
    fp = 0
    tn = 0
    fn = 0

    wrongLocation = 0

    commitPatches = 0
    commitSuccess = 0

    file = 0
    fileSuccess = 0

    line = 0
    lineSuccess = 0

    def getNumFilePatchFailures(self):
        return self.file - self.fileSuccess

    def getNumLinePatchFailures(self):
        return self.line - self.lineSuccess

    def getNumCommitFailures(self):
        return self.commitPatches - self.commitSuccess

class Experiment:
    normal = PatchStrategy()
    filtered = PatchStrategy()
