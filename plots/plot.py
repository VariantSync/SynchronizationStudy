import matplotlib.pyplot as plt
from matplotlib.sankey import Sankey
import os
import numpy


OUTPUT_FORMAT = ".pdf"
# OUTPUT_FORMAT = ".pdf"
DPI = 300


def toThousandsFormattedString(intval):
    return f"{intval:,}"


def percentageToAmount(percentage, total):
    # Is round correct here?
    return round((total * float(percentage)) / 100.0)


def labelPrecentageAndAmount(total):
    return lambda percentage: str(round(percentage,2)) + "%\n" + toThousandsFormattedString(percentageToAmount(percentage, total)) + " patches"
def labelPrecentage():
    return lambda percentage: '{:.1f}%'.format(percentage)


def piechart(labels, colors, sizes, labelfunction, outputPath, innerlabelfix=lambda autotexts:{}, outerlabelfix=lambda texts:{}):
    plt.rc('font', size=14)
    # explode = (0, 0.1, 0, 0)  # only "explode" the 2nd slice (i.e. 'Hogs')

    fig1, ax1 = plt.subplots()
    patches, texts, autotexts = ax1.pie(
        sizes,
        # explode=numpy.full(shape=len(sizes), fill_value=0.02, dtype=numpy.float),
        labels=labels,
        colors=colors,
        # how to produce inner labels of the pie pieces. We take the percentage of the pie we get, display it and the total value.
        autopct=labelfunction,
        shadow=False,
        counterclock=True,
        startangle=90,
        wedgeprops = {"edgecolor" : "white",
                      'linewidth': 1,
                      'antialiased': True})
    ax1.axis('equal')  # Equal aspect ratio ensures that pie is drawn as a circle.
    innerlabelfix(autotexts)
    outerlabelfix(texts)

    plt.savefig(outputPath, dpi=DPI, bbox_inches='tight')


def rq1_piechart(failure, success, outputPath):
    labels = 'Failure', 'Success'
    colors = 'darkorange' , 'forestgreen'
    sizes = [failure, success]
    piechart(labels, colors, sizes, labelPrecentageAndAmount(numpy.sum(sizes)), outputPath)


def accuracy_piecharts(patchstrategy, colourscheme, rq, outDir, innerlabelfix1=lambda texts:{}, outerlabelfix1=lambda texts:{}):
    labels = 'TP (correct)', 'FP (invalid)', 'FN (wrong location)'
    colors =  colourscheme.tp, colourscheme.fp, colourscheme.fn_wronglocation
    sizes = [patchstrategy.tp, patchstrategy.fp, patchstrategy.wrongLocation]
    piechart(labels, colors, sizes, labelPrecentage(), os.path.join(outDir, patchstrategy.name + "_" + rq + "_applicable" + OUTPUT_FORMAT), innerlabelfix=innerlabelfix1, outerlabelfix=outerlabelfix1)

    labels = 'TN (not required)', 'FN (missing)'
    colors =  colourscheme.tn, colourscheme.fn_missing
    sizes = [patchstrategy.tn, patchstrategy.fn - patchstrategy.wrongLocation]
    piechart(labels, colors, sizes, labelPrecentage(), os.path.join(outDir, patchstrategy.name + "_" + rq + "_failed" + OUTPUT_FORMAT))


def rq2_innerlabelfix1(autotexts):
    autotexts[1]._y = autotexts[1]._y - 0.08
def rq2_piechart(patchstrategy, colourscheme, outDir):
    accuracy_piecharts(patchstrategy, colourscheme, "rq2", outDir, innerlabelfix1=rq2_innerlabelfix1)


def rq3_innerlabelfix1(autotexts):
    autotexts[1]._y = autotexts[1]._y - 0.1
def rq3_outerlabelfix1(texts):
    texts[0]._y = texts[0]._y + 0.09
    texts[1]._y = texts[1]._y - 0.09
def rq3_piechart(filtered, colourscheme, outDir):
    accuracy_piecharts(filtered, colourscheme, "rq3", outDir, innerlabelfix1=rq3_innerlabelfix1, outerlabelfix1=rq3_outerlabelfix1)

def rq3_granularity_innerlabelfix(autotexts):
    autotexts[0]._y = autotexts[0]._y + 0.05
    autotexts[1]._y = autotexts[1]._y - 0.1
def rq3_granularity_outerlabelfix(texts):
    texts[2]._y = texts[2]._y + 0.08
def rq3_granularity_piechart(experiment, outDir):
    labels = 'Failure', 'Failure but filtered', 'Success but filtered', 'Success'
    colors = 'darkorange', 'navajowhite', 'lightgreen', 'forestgreen'

    success = experiment.filtered.lineSuccess
    successFiltered = experiment.normal.lineSuccess - success
    failure = experiment.filtered.getNumLinePatchFailures()
    failureFiltered = experiment.normal.getNumLinePatchFailures() - failure

    sizes = [failure, failureFiltered, successFiltered, success]
    piechart(labels, colors, sizes, labelPrecentageAndAmount(numpy.sum(sizes)), os.path.join(outDir, experiment.filtered.name + "_lines" + OUTPUT_FORMAT), innerlabelfix=rq3_granularity_innerlabelfix, outerlabelfix=rq3_granularity_outerlabelfix)


def rq3_barchart(experiment, colourscheme, outDir):
    n = experiment.normal
    f = experiment.filtered

    labels = ['TP', 'FP', 'TN', 'FN']
    nvals = [n.tp, n.fp, n.tn, n.fn]
    ntotal = numpy.sum(nvals)
    fvals = [f.tp, f.fp, f.tn, f.fn]
    ftotal = numpy.sum(fvals)

    # normalize values
    # def normalize(vals, total):
    #     return list(map(lambda x: float(x)/float(total), vals))
    # nvals = normalize(nvals, ntotal)
    # fvals = normalize(fvals, ftotal)

    x = numpy.arange(len(labels))
    widthOfBars = 0.35

    fig, ax = plt.subplots()
    nrects = ax.bar(x - widthOfBars/2, nvals, widthOfBars, label='Normal')
    frects = ax.bar(x + widthOfBars/2, fvals, widthOfBars, label='Filtered')
    
    # Add some text for labels, title and custom x-axis tick labels, etc.
    ax.set_ylabel('Number of Patches')
    # ax.set_yscale('log')
    # ax.set_title('Scores by group and gender')
    ax.set_xticks(x)
    ax.set_xticklabels(labels)
    ax.legend()

    ax.bar_label(nrects,
        labels=map(lambda x: "", nvals),
        padding=3)
    ax.bar_label(frects,
        labels=map(lambda x: "", fvals),
        padding=3)

    fig.tight_layout()

    plt.savefig(os.path.join(outDir, "metrics" + OUTPUT_FORMAT), dpi=DPI, bbox_inches='tight')


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
    rq1_piechart(patchstrategy.getNumCommitFailures(),    patchstrategy.commitSuccess, os.path.join(outDir, patchstrategy.name + "_commit" + OUTPUT_FORMAT))
    rq1_piechart(patchstrategy.getNumFilePatchFailures(), patchstrategy.fileSuccess,   os.path.join(outDir, patchstrategy.name + "_file" + OUTPUT_FORMAT))
    rq1_piechart(patchstrategy.getNumLinePatchFailures(), patchstrategy.lineSuccess,   os.path.join(outDir, patchstrategy.name + "_lines" + OUTPUT_FORMAT))


def rq2(patchstrategy, colourscheme, outDir):
    print("RQ2")
    rq2_piechart(patchstrategy, colourscheme, outDir)


def rq3(experiment, colourscheme, outDir):
    print("RQ3")
    rq3_piechart(experiment.filtered, colourscheme, outDir)
    rq3_granularity_piechart(experiment, outDir)
    rq3_barchart(experiment, colourscheme, outDir)
    # sankey(patchstrategy)
