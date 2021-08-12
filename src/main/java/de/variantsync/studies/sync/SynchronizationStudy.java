package de.variantsync.studies.sync;

import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureModelElement;
import de.variantsync.evolution.feature.Sample;
import de.variantsync.evolution.feature.Variant;
import de.variantsync.evolution.feature.config.FeatureIDEConfiguration;
import de.variantsync.evolution.feature.sampling.FeatureIDESampler;
import de.variantsync.evolution.io.Resources;
import de.variantsync.evolution.io.data.VariabilityDatasetLoader;
import de.variantsync.evolution.repository.SPLRepository;
import de.variantsync.evolution.util.CaseSensitivePath;
import de.variantsync.evolution.util.LogLevel;
import de.variantsync.evolution.util.Logger;
import de.variantsync.evolution.util.fide.FeatureModelUtils;
import de.variantsync.evolution.util.functional.Result;
import de.variantsync.evolution.variability.CommitPair;
import de.variantsync.evolution.variability.SPLCommit;
import de.variantsync.evolution.variability.VariabilityDataset;
import de.variantsync.evolution.variability.pc.Artefact;
import de.variantsync.evolution.variability.pc.VariantGenerationOptions;
import de.variantsync.evolution.variability.pc.groundtruth.GroundTruth;
import de.variantsync.studies.sync.diff.DiffParser;
import de.variantsync.studies.sync.diff.components.FineDiff;
import de.variantsync.studies.sync.diff.components.OriginalDiff;
import de.variantsync.studies.sync.diff.filter.EditFilter;
import de.variantsync.studies.sync.diff.filter.NaiveFilter;
import de.variantsync.studies.sync.diff.splitting.DefaultContextProvider;
import de.variantsync.studies.sync.diff.splitting.DiffSplitter;
import de.variantsync.studies.sync.diff.splitting.IContextProvider;
import de.variantsync.studies.sync.error.Panic;
import de.variantsync.studies.sync.error.ShellException;
import de.variantsync.studies.sync.experiment.BusyboxPreparation;
import de.variantsync.studies.sync.experiment.PatchOutcome;
import de.variantsync.studies.sync.experiment.ResultAnalysis;
import de.variantsync.studies.sync.shell.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class SynchronizationStudy {
    // TODO: Set in external config
    private static final Path datasetPath = Path.of("/home/alex/data/synchronization-study/better-dataset/VariabilityExtraction/extraction-results/busybox/output");
    private static final Path workDir = Path.of("/home/alex/data/synchronization-study/workdir");
    private static final Path debugDir = workDir.resolve("DEBUG");
    private static final int randomRepeats = 1;
    private static final int numVariants = 5;
    private static final String DATASET = "BUSYBOX";
    private static final Path resultFile = workDir.resolve("results.txt");
    private static final Path splRepositoryPath = workDir.getParent().resolve("BAK_busybox");
    private static final Path splRepositoryV0Path = workDir.resolve("busybox-V0");
    private static final Path splRepositoryV1Path = workDir.resolve("busybox-V1");
    private static final CaseSensitivePath variantsDirV0 = new CaseSensitivePath(workDir.resolve("V0Variants"));
    private static final CaseSensitivePath variantsDirV1 = new CaseSensitivePath(workDir.resolve("V1Variants"));
    private static final Path patchDir = workDir.resolve("TARGET");
    private static final Path normalPatchFile = workDir.resolve("patch.txt");
    private static final Path filteredPatchFile = workDir.resolve("filtered-patch.txt");
    private static final Path rejectsNormalFile = workDir.resolve("rejects-normal.txt");
    private static final Path rejectsFilteredFile = workDir.resolve("rejects-filtered.txt");
    private static final FeatureIDESampler variantSampler = FeatureIDESampler.CreateRandomSampler(numVariants);
    private static final ShellExecutor shell = new ShellExecutor(Logger::debug, Logger::error, workDir);
    private static final LogLevel logLevel = LogLevel.STATUS;


    public static void main(String... args) {
        Set<CommitPair<SPLCommit>> pairs = init();
        Random random = new Random();

        // Initialize the SPL repositories for different versions
        Logger.status("Initializing SPL repos.");
        final SPLRepository splRepositoryV0 = new SPLRepository(splRepositoryV0Path);
        final SPLRepository splRepositoryV1 = new SPLRepository(splRepositoryV1Path);

        // For each pair
        Logger.status("Starting diffing and patching...");
        long runID = 0;
        int pairCount = 0;
        for (CommitPair<SPLCommit> pair : pairs) {
            // Take next commit pair
            SPLCommit commitV0 = pair.parent();
            SPLCommit commitV1 = pair.child();

            splRepoPreparation(splRepositoryV0, splRepositoryV1, commitV0, commitV1);
            Logger.status("Loading feature models.");
            IFeatureModel modelV0 = commitV0.featureModel().run().orElseThrow();
            IFeatureModel modelV1 = commitV1.featureModel().run().orElseThrow();
            // We use the union of both models to sample configurations, so that all features are included
            Logger.status("Creating model union.");
            IFeatureModel modelUnion = FeatureModelUtils.UnionModel(modelV0, modelV1);
            Collection<String> featuresInDifference = FeatureModelUtils.getSymmetricFeatureDifference(modelV0, modelV1);

            // While more random configurations to consider
            for (int i = 0; i < randomRepeats; i++) {
                Logger.status("Starting repetition " + (i + 1) + " of " + randomRepeats + " with (random) variants.");
                // Sample set of random variants
                Logger.status("Sampling next set of variants...");
                Sample sample = variantSampler.sample(modelUnion);
                Logger.status("Done. Sampled " + sample.variants().size() + " variants.");

                if (Files.exists(debugDir)) {
                    shell.execute(new RmCommand(debugDir).recursive());
                }
                if (debugDir.toFile().mkdirs()) {
                    Logger.warning("Created Debug directory.");
                }
                if (Files.exists(variantsDirV0.path())) {
                    Logger.status("Cleaning variants dir V0.");
                    shell.execute(new RmCommand(variantsDirV0.path()).recursive());
                }
                if (Files.exists(variantsDirV1.path())) {
                    Logger.status("Cleaning variants dir V1.");
                    shell.execute(new RmCommand(variantsDirV1.path()).recursive());
                }

                // Write information about the commits
                featureModelDebug(commitV0, commitV1, modelV0, modelV1, featuresInDifference);

                // Generate the randomly selected variants at both versions
                Map<Variant, GroundTruth> groundTruthV0 = new HashMap<>();
                Map<Variant, GroundTruth> groundTruthV1 = new HashMap<>();
                Logger.status("Generating variants...");
                for (Variant variant : sample.variants()) {
                    generateVariant(commitV0, commitV1, modelUnion, groundTruthV0, groundTruthV1, variant);
                }
                Logger.status("Done.");

                // Select a random source variant
                Variant source = sample.variants().get(random.nextInt(sample.size()));
                Logger.status("Starting diff application for source variant " + source.getName());
                if (Files.exists(normalPatchFile)) {
                    Logger.status("Cleaning old patch file " + normalPatchFile);
                    shell.execute(new RmCommand(normalPatchFile));
                }
                // Apply diff to both versions of source variant
                Logger.info("Diffing source...");
                OriginalDiff originalDiff = getOriginalDiff(variantsDirV0.path().resolve(source.getName()), variantsDirV1.path().resolve(source.getName()));
                if (originalDiff.isEmpty()) {
                    // There was no change to this variant, so we can skip it as source
                    Logger.status("Skipping " + source + " as diff source. Diff is empty");
                    continue;
                } else {
                    try {
                        Files.write(debugDir.resolve("diff.txt"), originalDiff.toLines());
                    } catch (IOException e) {
                        Logger.error("Was not able to save diff", e);
                    }
                }
                Logger.info("Converting diff...");
                // Convert the original diff into a fine diff
                FineDiff normalPatch = getFineDiff(originalDiff);
                saveDiff(normalPatch, normalPatchFile);
                Logger.info("Saved fine diff.");

                // For each target variant,
                Logger.status("Starting patch application for source variant " + source.getName());
                for (Variant target : sample.variants()) {
                    if (target == source) {
                        continue;
                    }
                    Logger.status("Considering variant " + target.getName() + " as next target.");
                    Path pathToTarget = variantsDirV0.path().resolve(target.getName());
                    Path pathToExpectedResult = variantsDirV1.path().resolve(target.getName());

                    /* Application of patches without knowledge about features */
                    Logger.info("Applying patch without knowledge about features...");
                    // Apply the fine diff to the target variant
                    List<Path> skippedNormal = applyPatch(normalPatchFile, pathToTarget, rejectsNormalFile);
                    // Evaluate the patch result
                    OriginalDiff actualVsExpectedNormal = getOriginalDiff(patchDir, pathToExpectedResult);
                    OriginalDiff rejectsNormal = readRejects(rejectsNormalFile);

                    /* Application of patches with knowledge about PC of edit only */
                    Logger.info("Applying patch with knowledge about edits' PCs...");
                    // Create target variant specific patch that respects PCs
                    FineDiff filteredPatch = getFilteredDiff(originalDiff, groundTruthV0.get(source).artefact(), groundTruthV1.get(source).artefact(), target);
                    boolean emptyPatch = filteredPatch.content().isEmpty();
                    saveDiff(filteredPatch, filteredPatchFile);
                    // Apply the patch
                    List<Path> skippedFiltered = applyPatch(filteredPatchFile, pathToTarget, rejectsFilteredFile, emptyPatch);
                    assert skippedFiltered.isEmpty();
                    // Evaluate the result
                    OriginalDiff actualVsExpectedFiltered = getOriginalDiff(patchDir, pathToExpectedResult);
                    OriginalDiff rejectsFiltered = readRejects(rejectsFilteredFile);

                    /* Result Evaluation */
                    FineDiff requiredPatch = getRequiredDiff(originalDiff, groundTruthV0.get(source).artefact(), groundTruthV1.get(source).artefact(), target);
                    PatchOutcome patchOutcome = ResultAnalysis.processOutcome(
                            DATASET,
                            runID,
                            source.getName(),
                            target.getName(),
                            commitV0, commitV1,
                            normalPatch, filteredPatch,
                            requiredPatch,
                            actualVsExpectedNormal, actualVsExpectedFiltered,
                            rejectsNormal, rejectsFiltered,
                            skippedNormal);

                    try {
                        patchOutcome.writeAsJSON(resultFile, true);
                    } catch (IOException e) {
                        Logger.error("Was not able to write filtered patch result file for run " + runID, e);
                    }

                    Logger.info("Finished patching for source " + source.getName() + " and target " + target.getName());
                    runID++;
                }
            }
            pairCount++;
            Logger.status(String.format("Finished commit pair %d of %d.%n", pairCount, pairs.size()));
        }
    }

    private static FineDiff getRequiredDiff(OriginalDiff originalDiff, Artefact tracesV0, Artefact tracesV1, Variant target) {
        NaiveFilter filter = new NaiveFilter(tracesV0, tracesV1, target, variantsDirV0.path(), variantsDirV1.path(), 2);
        // Create target variant specific patch that respects PCs
        IContextProvider contextProvider = new DefaultContextProvider(workDir);
        return DiffSplitter.split(originalDiff, filter, filter, contextProvider);
    }

    @Nullable
    private static OriginalDiff readRejects(Path rejectFile) {
        OriginalDiff rejectsDiff = null;
        if (Files.exists(rejectFile)) {
            try {
                List<String> rejects = Files.readAllLines(rejectFile);
                rejectsDiff = DiffParser.toOriginalDiff(rejects);
            } catch (IOException e) {
                Logger.error("Was not able to read rejects file.", e);
            }
        }
        return rejectsDiff;
    }

    private static void featureModelDebug(SPLCommit commitV0, SPLCommit commitV1, IFeatureModel modelV0, IFeatureModel modelV1, Collection<String> featuresInDifference) {
        try {
            Resources.Instance().write(Artefact.class, commitV0.presenceConditions().run().get(), debugDir.resolve("V0.spl.csv"));
            Resources.Instance().write(Artefact.class, commitV1.presenceConditions().run().get(), debugDir.resolve("V1.spl.csv"));
            Files.write(debugDir.resolve("features-V0.txt"), modelV0.getFeatures().stream().map(IFeatureModelElement::getName).collect(Collectors.toSet()));
            Files.write(debugDir.resolve("features-V1.txt"), modelV1.getFeatures().stream().map(IFeatureModelElement::getName).collect(Collectors.toSet()));
            Files.write(debugDir.resolve("variables-in-difference.txt"), featuresInDifference);
        } catch (Resources.ResourceIOException | IOException e) {
            Logger.error("Was not able to write commit data.");
        }
    }

    private static void generateVariant(SPLCommit commitV0, SPLCommit commitV1, IFeatureModel modelUnion, Map<Variant, GroundTruth> groundTruthV0, Map<Variant, GroundTruth> groundTruthV1, Variant variant) {
        Logger.status("Generating variant " + variant.getName());
        if (variant.getConfiguration() instanceof FeatureIDEConfiguration config) {
            try {
                Files.write(debugDir.resolve(variant.getName() + ".config"), config.toAssignment().entrySet().stream().map(entry -> entry.getKey() + " : " + entry.getValue()).collect(Collectors.toList()));
            } catch (IOException e) {
                Logger.error("Was not able to write configuration of " + variant.getName(), e);
            }
        }

        // TODO: Check whether this is enough. It only checks satisfiability, but might not check whether the variables exist in the feature model. Maybe check whether the set of selected variables exists?
        if (!variant.isImplementing(new FeatureModelFormula(modelUnion).getPropositionalNode())) {
            panic("Sampled " + variant + " is not valid for feature model!");
        }

        GroundTruth gtV0 = commitV0
                .presenceConditions()
                .run()
                .orElseThrow()
                .generateVariant(
                        variant,
                        new CaseSensitivePath(splRepositoryV0Path),
                        variantsDirV0.resolve(variant.getName()),
                        VariantGenerationOptions.ExitOnError)
                .expect("Was not able to generate V0 of " + variant);
        try {
            Resources.Instance().write(Artefact.class, gtV0.artefact(), debugDir.resolve("V0-" + variant.getName() + ".variant.csv"));
        } catch (Resources.ResourceIOException e) {
            Logger.error("Was not able to write ground truth.");
        }
        groundTruthV0.put(variant, gtV0);

        GroundTruth gtV1 = commitV1
                .presenceConditions()
                .run()
                .orElseThrow()
                .generateVariant(
                        variant,
                        new CaseSensitivePath(splRepositoryV1Path),
                        variantsDirV1.resolve(variant.getName()),
                        VariantGenerationOptions.ExitOnError)
                .expect("Was not able to generate V1 of " + variant);
        try {
            Resources.Instance().write(Artefact.class, gtV1.artefact(), debugDir.resolve("V1-" + variant.getName() + ".variant.csv"));
        } catch (Resources.ResourceIOException e) {
            Logger.error("Was not able to write ground truth.", e);
        }
        groundTruthV1.put(variant, gtV1);
    }

    private static void splRepoPreparation(SPLRepository splRepositoryV0, SPLRepository splRepositoryV1, SPLCommit commitV0, SPLCommit commitV1) {
        Logger.info("Next V0 commit: " + commitV0);
        Logger.info("Next V1 commit: " + commitV1);
        // Checkout the commits in the SPL repository
        try {
            // Stash all changes and drop the stash. This is a workaround as the JGit API does not support restore.
            Logger.status("Cleaning state of V0 repo.");
            splRepositoryV0.stashCreate(true);
            splRepositoryV0.dropStash();
            Logger.status("Cleaning state of V1 repo.");
            splRepositoryV1.stashCreate(true);
            splRepositoryV1.dropStash();

            Logger.status("Checkout of commits in SPL repo.");
            splRepositoryV0.checkoutCommit(commitV0, true);
            splRepositoryV1.checkoutCommit(commitV1, true);

        } catch (GitAPIException | IOException e) {
            panic("Was not able to checkout commit for SPL repository.", e);
        }
        Logger.info("Done.");

        if (DATASET.equals("BUSYBOX")) {
            Logger.status("Normalizing BusyBox files...");
            try {
                BusyboxPreparation.normalizeDir(splRepositoryV0Path.toFile());
                BusyboxPreparation.normalizeDir(splRepositoryV1Path.toFile());
            } catch (IOException e) {
                Logger.error("", e);
                panic("Was not able to normalize BusyBox.", e);
            }
        }
    }

    private static Set<CommitPair<SPLCommit>> init() {
        // Initialize the library
        de.variantsync.evolution.Main.Initialize();
        Logger.status("Starting experiment initialization.");
        Logger.setLogLevel(logLevel);
        // Clean old SPL repo files
        Logger.status("Cleaning old repo files.");
        if (Files.exists(splRepositoryV0Path)) {
            shell.execute(new RmCommand(splRepositoryV0Path).recursive()).expect("Was not able to remove SPL-V0.");
        }
        if (Files.exists(splRepositoryV1Path)) {
            shell.execute(new RmCommand(splRepositoryV1Path).recursive()).expect("Was not able to remove SPL-V1.");
        }
        // Copy the SPL repo
        Logger.status("Creating new SPL repo copies.");
        shell.execute(new CpCommand(splRepositoryPath, splRepositoryV0Path).recursive()).expect("Was not able to copy SPL-V0.");
        shell.execute(new CpCommand(splRepositoryPath, splRepositoryV1Path).recursive()).expect("Was not able to copy SPL-V1.");


        // Load VariabilityDataset
        Logger.status("Loading variability dataset.");
        VariabilityDataset dataset = null;
        try {
            Resources instance = Resources.Instance();
            final VariabilityDatasetLoader datasetLoader = new VariabilityDatasetLoader();
            instance.registerLoader(VariabilityDataset.class, datasetLoader);
            dataset = instance.load(VariabilityDataset.class, datasetPath);
        } catch (Resources.ResourceIOException e) {
            panic("Was not able to load dataset.", e);
        }

        // Retrieve pairs/sequences of usable commits
        Logger.status("Retrieving commit pairs");
        return Objects.requireNonNull(dataset).getCommitPairsForEvolutionStudy();
    }

    private static void saveDiff(FineDiff fineDiff, Path file) {
        // Save the fine diff to a file
        try {
            Files.write(file, fineDiff.toLines());
        } catch (IOException e) {
            panic("Was not able to save diff to file " + file);
        }
    }

    private static List<Path> applyPatch(Path patchFile, Path targetVariant, Path rejectFile) {
        return applyPatch(patchFile, targetVariant, rejectFile, false);
    }

    private static List<Path> applyPatch(Path patchFile, Path targetVariant, Path rejectFile, boolean emptyPatch) {
        // Clean patch directory
        if (Files.exists(patchDir.toAbsolutePath())) {
            shell.execute(new RmCommand(patchDir.toAbsolutePath()).recursive());
        }

        if (Files.exists(rejectFile)) {
            Logger.info("Cleaning old rejects file " + rejectFile);
            shell.execute(new RmCommand(rejectFile));
        }

        // copy target variant
        shell.execute(new CpCommand(targetVariant, patchDir).recursive()).expect("Was not able to copy variant " + targetVariant);

        // apply patch to copied target variant
        List<Path> skipped = new LinkedList<>();
        if (!emptyPatch) {
            Result<List<String>, ShellException> result = shell.execute(PatchCommand.Recommended(patchFile).strip(2).rejectFile(rejectFile).force(), patchDir);
            if (result.isSuccess()) {
                result.getSuccess().forEach(Logger::info);
            } else {
                List<String> lines = result.getFailure().getOutput();
                Logger.error("Failed to apply part of patch.");
                String oldFile;
                for (String nextLine : lines) {
                    if (nextLine.startsWith("|---")) {
                        oldFile = nextLine.split("\\s+")[1];
                        skipped.add(Path.of(oldFile));
                    }
                }
            }
        }
        return skipped;
    }

    @NotNull
    private static FineDiff getFineDiff(OriginalDiff originalDiff) {
        DefaultContextProvider contextProvider = new DefaultContextProvider(workDir);
        return DiffSplitter.split(originalDiff, contextProvider);
    }

    private static FineDiff getFilteredDiff(OriginalDiff originalDiff, Artefact tracesV0, Artefact tracesV1, Variant target) {
        EditFilter editFilter = new EditFilter(tracesV0, tracesV1, target, variantsDirV0.path(), variantsDirV1.path(), 2);
        // Create target variant specific patch that respects PCs
        IContextProvider contextProvider = new DefaultContextProvider(workDir);
        return DiffSplitter.split(originalDiff, editFilter, editFilter, contextProvider);
    }

    private static OriginalDiff getOriginalDiff(Path v0Path, Path v1Path) {
        DiffCommand diffCommand = DiffCommand.Recommended(workDir.relativize(v0Path), workDir.relativize(v1Path));
        List<String> output = shell.execute(diffCommand, workDir).expect("Was not able to diff variants.");
        return DiffParser.toOriginalDiff(output);
    }

    private static void panic(String message) {
        Logger.error(message);
        throw new Panic(message);
    }

    private static void panic(String message, Exception e) {
        Logger.error(message, e);
        e.printStackTrace();
        throw new Panic(message);
    }


}