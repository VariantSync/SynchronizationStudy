import matplotlib.pyplot as plt
from matplotlib.sankey import Sankey
import os
import numpy


def toThousandsFormattedString(intval):
    return f"{intval:,}"


def percentageToAmount(percentage, total):
    # Is round correct here?
    return round((total * float(percentage)) / 100.0)


def piechart(failure, success, outputPath, dpi=300):
    plt.rc('font', size=14)

    labels = 'Failure', 'Success'
    colors = 'darkorange' , 'forestgreen'
    sizes = [failure, success]
    total = numpy.sum(sizes)
    # explode = (0, 0.1, 0, 0)  # only "explode" the 2nd slice (i.e. 'Hogs')

    fig1, ax1 = plt.subplots()
    ax1.pie(
        sizes,
        # explode=explode,
        labels=labels,
        colors=colors,
        # how to produce inner labels of the pie pieces. We take the percentage of the pie we get, display it and the total value.
        autopct=lambda percentage: str(round(percentage,2)) + "%\n" + toThousandsFormattedString(percentageToAmount(percentage, total)) + " patches",#'%1.1f%%',
        shadow=False,
        counterclock=True,
        startangle=90)

    ax1.axis('equal')  # Equal aspect ratio ensures that pie is drawn as a circle.

    plt.savefig(outputPath, dpi=dpi, bbox_inches='tight')


def sankey(patchstrategy):
    # first row
    numPatches = float(patchstrategy.line)

    # second row
    numApplicable = float(patchstrategy.lineSuccess) # from numPatches
    numFailed = float(patchstrategy.getNumLinePatchFailures()) # from numPatches
    # numApplicable = numApplicable / numPatches
    # numFailed = numFailed / numPatches
    # numPatches = 1.0

    # third row
    numCorrect = patchstrategy.tp # from numApplicable
    numWrongLocation = patchstrategy.wrongLocation # from numApplicable
    numInvalid = patchstrategy.fp # from numApplicable
    numNotRequired = patchstrategy.tn # from numFailed
    numMissing = patchstrategy.fn - numWrongLocation # from numFailed

    print("numApplicable", numApplicable)
    print("numFailed", numFailed)
    print()
    print("numCorrect", numCorrect)
    print("numWrongLocation", numWrongLocation)
    print("numInvalid", numInvalid)
    print("numCorrect + numWrongLocation + numInvalid", numCorrect + numWrongLocation + numInvalid)
    print("numNotRequired", numNotRequired)
    print("numMissing", numMissing)
    print("numNotRequired + numMissing", numNotRequired + numMissing)
    print()

    # fourth row
    tp = patchstrategy.tp # from numCorrect
    fp = patchstrategy.fp # from numInvalid
    tn = patchstrategy.tn # from numNotRequired
    fn = patchstrategy.fn # from numWrongLocation + numMissing

    ### plotting

    # fig = plt.figure()
    # ax = fig.add_subplot(1, 1, 1, xticks=[], yticks=[],
    #                     title="Flow Diagram of a Widget")

    sankey = Sankey(
        # ax=ax,
        unit='',
        scale=1.0 / numPatches,
        # offset=0.2,
        head_angle=150,
        # format='%.0f',
        format = '',
        shoulder = 0
        )

    flows1  = [numPatches, -numApplicable, -numFailed]
    labels1 = ['All Patches', 'Applicable', 'Failed']
    print(labels1)
    print(flows1)
    sankey.add(
        flows=flows1,
        labels=labels1,
        orientations=[0, 0, 0],
        # pathlengths=[float(numApplicable) / float(numPatches), float(numApplicable) / float(numFailed)]
        # , patchlabel="Widget\nA"  # Arguments to matplotlib.patches.PathPatch
        )

    flows2  = [numApplicable, -numCorrect, -numWrongLocation, numFailed, -numInvalid, -numNotRequired, -numMissing]
    labels2 = ['', 'Correct', 'Wrong Location', '', 'Invalid', 'Not Required', 'Missing']
    # print(labels2)
    # print(flows2)
    sankey.add(
        flows=flows2,
        labels=labels2,
        orientations=[0, 0, 0, 0, 0, 0, 0]
        , prior=0
        , connect=(1, 0)
    )

    diagrams = sankey.finish()
    # diagrams[0].texts[-1].set_color('r')
    # diagrams[0].text.set_fontweight('bold')
    plt.show()


def rq1(patchstrategy, outDir):
    print("RQ1")
    piechart(patchstrategy.getNumCommitFailures(),    patchstrategy.commitSuccess, os.path.join(outDir, patchstrategy.name + "_commit.png"))
    piechart(patchstrategy.getNumFilePatchFailures(), patchstrategy.fileSuccess,   os.path.join(outDir, patchstrategy.name + "_file.png"))
    piechart(patchstrategy.getNumLinePatchFailures(), patchstrategy.lineSuccess,   os.path.join(outDir, patchstrategy.name + "_lines.png"))


def rq2(patchstrategy, outDir):
    print("RQ2")
    sankey(patchstrategy)


def rq3(patchstrategy, outDir):
    print("RQ3")
    sankey(patchstrategy)
