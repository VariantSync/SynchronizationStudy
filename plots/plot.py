import matplotlib.pyplot as plt
from matplotlib.sankey import Sankey
import os
import numpy


# OUTPUT_FORMAT = ".png"
OUTPUT_FORMAT = ".pdf"
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


def rq1_barchart(experiment, outputPath):
    colourscheme = 'forestgreen', 'darkorange'
    commit_success = experiment.commitSuccess
    commit_failed = experiment.getNumCommitFailures()
    file_success = experiment.fileSuccess
    file_failed = experiment.getNumFilePatchFailures()
    line_success = experiment.lineSuccess
    line_failed = experiment.getNumLinePatchFailures()

    labels = ['(a) commit-sized', '(b) file-sized', '(c) line-sized']
    success_vals = [commit_success, file_success, line_success]
    failed_vals = [commit_failed, file_failed, line_failed]
    sum_vals = numpy.sum([success_vals, failed_vals], axis=0)
    # print(numpy.sum(fvals))

    # normalize values
    # def normalize(vals, total):
    #     return list(map(lambda x: float(x)/float(total), vals))
    # nvals = normalize(nvals, ntotal)
    # fvals = normalize(fvals, ftotal)

    _scalefactor = 1
    x = numpy.arange(_scalefactor * len(labels), step=_scalefactor)
    # print(x)
    widthOfBars = 0.2

    fig, ax = plt.subplots()
    success_percentage = numpy.divide(success_vals, sum_vals)
    failed_percentage = numpy.divide(failed_vals, sum_vals)
    success_rects = ax.bar(x - 0.05 - widthOfBars / 2, 100*success_percentage, widthOfBars, label='applicable', color=colourscheme[0])
    failed_rects = ax.bar(x + 0.05 + widthOfBars / 2, 100*failed_percentage, widthOfBars, label='failed', color=colourscheme[1])

    def label_values(percentage, absolute, offset, c):
        for i, v in enumerate(percentage):
            v = 100*v
            label = str(numpy.round(v, 2))
            label += "%"
            ax.text(i+offset+0.1, v + 5, label, color=c, fontweight='bold')
            label = "({:,.0f})".format(absolute[i])
            ax.text(i+offset, v + 1, label, color=c, fontweight='bold')

    label_values(success_percentage, success_vals, -0.4, colourscheme[0])
    label_values(failed_percentage, failed_vals, -0.05, colourscheme[1])

    # Add some text for labels, title and custom x-axis tick labels, etc.
    ax.set_ylabel('Percentage of patches')
    ax.set_ylim([0, 100])
    ax.set_xlim([-0.5, 2.6])
    # ax.set_yscale('log')
    # ax.set_title('Scores by group and gender')
    ax.set_xticks(x)
    ax.set_xticklabels(labels)
    ax.legend(loc=1)

    ax.bar_label(success_rects,
                 labels=map(lambda x: "", success_vals),
                 padding=3)
    ax.bar_label(failed_rects,
                 labels=map(lambda x: "", failed_vals),
                 padding=3)

    fig.tight_layout()

    plt.savefig(outputPath, dpi=DPI, bbox_inches='tight')


def correctness_barchart(experiment, outputPath):
    colourscheme = 'forestgreen', 'darkorange'

    labels = ['correct\n(TP)', 'invalid\n(FP)', 'wrong location\n(FN)', 'not required\n(TN)', 'missing\n(FN)']
    success_vals = [experiment.tp, experiment.fp, experiment.wrongLocation]
    failed_vals = [experiment.tn, experiment.fn - experiment.wrongLocation]

    _scalefactor = 1
    s = numpy.arange(_scalefactor * len(success_vals), step=_scalefactor)
    start = _scalefactor * len(success_vals) + 0.3
    stop = start + _scalefactor * len(failed_vals)
    f = numpy.arange(start=start, stop=stop, step=_scalefactor)
    widthOfBars = 0.35

    fig, ax = plt.subplots()
    success_percentage = 100 * numpy.divide(success_vals, numpy.sum(success_vals))
    failed_percentage = 100 * numpy.divide(failed_vals, numpy.sum(failed_vals))
    success_rects = ax.bar(s, success_percentage, widthOfBars, label='applicable', color=colourscheme[0])
    failed_rects = ax.bar(f, failed_percentage, widthOfBars, label='failed', color=colourscheme[1])
    ax.vlines(2.65, ymin=0, ymax=105, color='black')

    def label_values(percentage, absolute, offset, c):
        for i, v in enumerate(percentage):
            v = v
            label = str(numpy.round(v, 2))
            label += "%"
            ax.text(i+offset+0.2, v + 5, label, color=c, fontweight='bold')
            label = "({:,.0f})".format(absolute[i])
            ax.text(i+offset, v + 1, label, color=c, fontweight='bold')

    label_values(success_percentage, success_vals, -0.42, colourscheme[0])
    label_values(failed_percentage, failed_vals, 2.72, colourscheme[1])

    # Add some text for labels, title and custom x-axis tick labels, etc.
    ax.set_ylabel('Percentage of patches')
    ax.set_ylim([0, 105])
    ax.set_xlim([-0.5, 5.2])
    # ax.set_yscale('log')
    # ax.set_title('Scores by group and gender')
    ax.set_xticks(numpy.append(s, f))
    ax.set_xticklabels(labels)
    ax.legend(loc=1)

    ax.bar_label(success_rects,
                 labels=map(lambda x: "", success_vals),
                 padding=3)
    ax.bar_label(failed_rects,
                 labels=map(lambda x: "", failed_vals),
                 padding=3)

    fig.tight_layout()

    plt.savefig(outputPath, dpi=DPI, bbox_inches='tight')


def accuracy_piecharts(patchstrategy, colourscheme, rq, outDir,
innerlabelfix1=lambda texts:{}, outerlabelfix1=lambda texts:{},
innerlabelfix2=lambda texts:{}, outerlabelfix2=lambda texts:{}
):
    labels = 'TP (correct)', 'FP (invalid)', 'FN (wrong location)'
    colors =  colourscheme.tp, colourscheme.fp, colourscheme.fn_wronglocation
    sizes = [patchstrategy.tp, patchstrategy.fp, patchstrategy.wrongLocation]
    piechart(labels, colors, sizes, labelPrecentage(), os.path.join(outDir, patchstrategy.name + "_" + rq + "_applicable" + OUTPUT_FORMAT), innerlabelfix=innerlabelfix1, outerlabelfix=outerlabelfix1)

    labels = 'TN (not required)', 'FN (missing)'
    colors =  colourscheme.tn, colourscheme.fn_missing
    sizes = [patchstrategy.tn, patchstrategy.fn - patchstrategy.wrongLocation]
    piechart(labels, colors, sizes, labelPrecentage(), os.path.join(outDir, patchstrategy.name + "_" + rq + "_failed" + OUTPUT_FORMAT), innerlabelfix=innerlabelfix2, outerlabelfix=outerlabelfix2)


def rq2_innerlabelfix1(autotexts):
    autotexts[2]._x = autotexts[2]._x + 0.07
    autotexts[1]._x = autotexts[1]._x + 0.01
    autotexts[2]._y = autotexts[2]._y + 0.2
    # autotexts[1]._y = autotexts[1]._y - 0.08
    autotexts[0].set_color('white')
def rq2_innerlabelfix2(autotexts):
    autotexts[1]._x = autotexts[1]._x + 0.05
    autotexts[1]._y = autotexts[1]._y + 0.2
    autotexts[1].set_color('white')
def rq2_piechart(patchstrategy, colourscheme, outDir):
    accuracy_piecharts(patchstrategy, colourscheme, "rq2", outDir, innerlabelfix1=rq2_innerlabelfix1, innerlabelfix2=rq2_innerlabelfix2)


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
    # print(numpy.sum(fvals))

    # normalize values
    # def normalize(vals, total):
    #     return list(map(lambda x: float(x)/float(total), vals))
    # nvals = normalize(nvals, ntotal)
    # fvals = normalize(fvals, ftotal)

    _scalefactor = 1
    x = numpy.arange(_scalefactor*len(labels), step = _scalefactor)
    # print(x)
    widthOfBars = 0.35

    fig, ax = plt.subplots()
    nrects = ax.bar(x - widthOfBars/2, nvals, widthOfBars, label='without domain knowledge')
    frects = ax.bar(x + widthOfBars/2, fvals, widthOfBars, label='with domain knowledge')
    
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
    rq1_barchart(patchstrategy, os.path.join(outDir, patchstrategy.name + "_applicability" + OUTPUT_FORMAT))
    # rq1_barchart(patchstrategy, os.path.join(outDir, patchstrategy.name + "_file" + OUTPUT_FORMAT))
    # rq1_barchart(patchstrategy, os.path.join(outDir, patchstrategy.name + "_lines" + OUTPUT_FORMAT))


def rq2(patchstrategy, colourscheme, outDir):
    print("RQ2")
    correctness_barchart(patchstrategy, os.path.join(outDir, patchstrategy.name + "_correctness" + OUTPUT_FORMAT))


def rq3(experiment, colourscheme, outDir):
    print("RQ3")
    # rq1_barchart(experiment.filtered, os.path.join(outDir, "filtered_applicability" + OUTPUT_FORMAT))
    rq3_piechart(experiment.filtered, colourscheme, outDir)
    rq3_granularity_piechart(experiment, outDir)
    rq3_barchart(experiment, colourscheme, outDir)
    # sankey(patchstrategy)
