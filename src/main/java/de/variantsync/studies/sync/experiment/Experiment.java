package de.variantsync.studies.sync.experiment;

import de.variantsync.evolution.feature.Variant;
import de.variantsync.evolution.feature.config.FeatureIDEConfiguration;
import de.variantsync.evolution.feature.sampling.Sample;
import de.variantsync.evolution.io.Resources;
import de.variantsync.evolution.io.data.VariabilityDatasetLoader;
import de.variantsync.evolution.repository.SPLRepository;
import de.variantsync.evolution.util.LogLevel;
import de.variantsync.evolution.util.Logger;
import de.variantsync.evolution.util.functional.Result;
import de.variantsync.evolution.util.io.CaseSensitivePath;
import de.variantsync.evolution.variability.EvolutionStep;
import de.variantsync.evolution.variability.SPLCommit;
import de.variantsync.evolution.variability.VariabilityDataset;
import de.variantsync.evolution.variability.pc.Artefact;
import de.variantsync.evolution.variability.pc.groundtruth.GroundTruth;
import de.variantsync.evolution.variability.pc.options.VariantGenerationOptions;
import de.variantsync.studies.sync.diff.DiffParser;
import de.variantsync.studies.sync.diff.components.FileDiff;
import de.variantsync.studies.sync.diff.components.FineDiff;
import de.variantsync.studies.sync.diff.components.OriginalDiff;
import de.variantsync.studies.sync.diff.filter.EditFilter;
import de.variantsync.studies.sync.diff.filter.IFileDiffFilter;
import de.variantsync.studies.sync.diff.filter.ILineFilter;
import de.variantsync.studies.sync.diff.splitting.DefaultContextProvider;
import de.variantsync.studies.sync.diff.splitting.DiffSplitter;
import de.variantsync.studies.sync.diff.splitting.IContextProvider;
import de.variantsync.studies.sync.error.Panic;
import de.variantsync.studies.sync.error.ShellException;
import de.variantsync.studies.sync.shell.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public abstract class Experiment {
    protected final Path workDir;
    protected final Path debugDir;
    protected final boolean inDebug;
    protected final Path resultFile;
    protected final Path splCopyA;
    protected final Path splCopyB;
    protected final CaseSensitivePath variantsDirV0;
    protected final CaseSensitivePath variantsDirV1;
    protected final Path patchDir;
    protected final Path normalPatchFile;
    protected final Path filteredPatchFile;
    protected final Path rejectsNormalFile;
    protected final Path rejectsFilteredFile;
    protected final ShellExecutor shell;
    protected final LogLevel logLevel;
    protected final int randomRepeats;
    protected final int numVariants;
    protected final EExperimentalSubject experimentalSubject;
    protected final Path splRepositoryPath;
    protected final Path datasetPath;
    protected final VariabilityDataset variabilityDataset;

    public Experiment(final ExperimentConfiguration config) {
        // Initialize the library
        de.variantsync.evolution.Main.Initialize();
        final Path mainDir = Path.of(config.EXPERIMENT_DIR_MAIN());
        try {
            if (mainDir.toFile().mkdirs()) {
                Logger.status("Created main directory " + mainDir);
            }
            workDir = Files.createTempDirectory(mainDir, "workdir");
        } catch (final IOException e) {
            Logger.error("Was not able to initialize workdir", e);
            throw new UncheckedIOException(e);
        }
        debugDir = workDir.resolve("DEBUG");
        inDebug = config.EXPERIMENT_DEBUG();
        resultFile = mainDir.resolve("results.txt");
        splRepositoryPath = Path.of(config.EXPERIMENT_DIR_SPL());
        datasetPath = Path.of(config.EXPERIMENT_DIR_DATASET());
        splCopyA = workDir.resolve("SPL-A");
        splCopyB = workDir.resolve("SPL-B");
        variantsDirV0 = new CaseSensitivePath(workDir.resolve("V0Variants"));
        variantsDirV1 = new CaseSensitivePath(workDir.resolve("V1Variants"));
        patchDir = workDir.resolve("TARGET/V0");
        normalPatchFile = workDir.resolve("patch.txt");
        filteredPatchFile = workDir.resolve("filtered-patch.txt");
        rejectsNormalFile = workDir.resolve("rejects-normal.txt");
        rejectsFilteredFile = workDir.resolve("rejects-filtered.txt");
        shell = new ShellExecutor(Logger::debug, Logger::error, workDir);
        logLevel = config.EXPERIMENT_LOGGER_LEVEL();
        randomRepeats = config.EXPERIMENT_REPEATS();
        numVariants = config.EXPERIMENT_VARIANT_COUNT();
        experimentalSubject = config.EXPERIMENT_SUBJECT();

        variabilityDataset = init();
    }

    @Nullable
    private static OriginalDiff readRejects(final Path rejectFile) {
        OriginalDiff rejectsDiff = null;
        if (Files.exists(rejectFile)) {
            try {
                final List<String> rejects = Files.readAllLines(rejectFile);
                rejectsDiff = DiffParser.toOriginalDiff(rejects);
            } catch (final IOException e) {
                Logger.error("Was not able to read rejects file.", e);
            }
        }
        return rejectsDiff;
    }

    public void run() {
        final Random random = new Random();

        // Initialize the SPL repositories for different versions
        Logger.status("Initializing SPL repos.");
        SPLRepository parentRepo = new SPLRepository(splCopyA);
        SPLRepository childRepo = new SPLRepository(splCopyB);

        // For each pair
        Logger.status("Starting diffing and patching...");
        long historySize = 0;
        for (var ignored : variabilityDataset.tidyStreamEvolutionSteps()) {
            historySize++;
        }
        long runID = 0;
        int pairCount = 0;
        var evolutionSteps = variabilityDataset.tidyStreamEvolutionSteps();
        for (EvolutionStep<SPLCommit> step : evolutionSteps) {
            // TODO: Switch the SPLRepository instances, so that only the new child commit has to be checked out
            // Increase one extra time for the first parent in the sequence
            pairCount++;
            SPLCommit parentCommit = step.parent();
            SPLCommit childCommit = step.child();
            final SimpleFileFilter fileFilter = splRepoPreparation(parentRepo, childRepo, parentCommit, childCommit);

            // While more random configurations to consider
            for (int i = 0; i < randomRepeats; i++) {
                Logger.warning("Starting repetition " + (i + 1) + " of " + randomRepeats + " with " + numVariants + " variants.");
                // Sample set of random variants
                Logger.status("Sampling next set of variants...");
                final Sample sample = sample(parentCommit, childCommit);
                Logger.status("Done. Sampled " + sample.variants().size() + " variants.");

                if (inDebug && Files.exists(debugDir)) {
                    shell.execute(new RmCommand(debugDir).recursive());
                }
                if (inDebug && debugDir.toFile().mkdirs()) {
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
                if (inDebug) {
                    try {
                        final var v0PCs = parentCommit.presenceConditions().run();
                        if (v0PCs.isPresent()) {
                            Resources.Instance().write(Artefact.class, v0PCs.get(), debugDir.resolve("V0.spl.csv"));
                        }

                        final var v1PCs = childCommit.presenceConditions().run();
                        if (v1PCs.isPresent()) {
                            Resources.Instance().write(Artefact.class, v1PCs.get(), debugDir.resolve("V1.spl.csv"));
                        }
                    } catch (final Resources.ResourceIOException e) {
                        panic("Was not able to write PCs", e);
                    }
                }

                // Generate the randomly selected variants at both versions
                final Map<Variant, GroundTruth> groundTruthV0 = new HashMap<>();
                final Map<Variant, GroundTruth> groundTruthV1 = new HashMap<>();
                Logger.status("Generating variants...");
                for (final Variant variant : sample.variants()) {
                    generateVariant(parentCommit, childCommit, groundTruthV0, groundTruthV1, variant, fileFilter);
                }
                Logger.status("Done.");

                // Select a random source variant
                final Variant source = sample.variants().get(random.nextInt(sample.size()));
                Logger.status("Starting diff application for source variant " + source.getName());
                if (Files.exists(normalPatchFile)) {
                    Logger.status("Cleaning old patch file " + normalPatchFile);
                    shell.execute(new RmCommand(normalPatchFile));
                }
                // Apply diff to both versions of source variant
                Logger.info("Diffing source...");
                final OriginalDiff originalDiff = getOriginalDiff(variantsDirV0.path().resolve(source.getName()), variantsDirV1.path().resolve(source.getName()));
                if (originalDiff.isEmpty()) {
                    // There was no change to this variant, so we can skip it as source
                    Logger.warning("Skipping " + source.getName() + " as diff source. Diff is empty.");
                    continue;
                } else if (inDebug) {
                    try {
                        Files.write(debugDir.resolve("diff.txt"), originalDiff.toLines());
                    } catch (final IOException e) {
                        Logger.error("Was not able to save diff", e);
                    }
                }
                Logger.info("Converting diff...");
                // Convert the original diff into a fine diff
                final FineDiff normalPatch = getFineDiff(originalDiff);
                saveDiff(normalPatch, normalPatchFile);
                Logger.info("Saved fine diff.");

                // For each target variant,
                Logger.status("Starting patch application for source variant " + source.getName());
                for (final Variant target : sample.variants()) {
                    if (target == source) {
                        continue;
                    }
                    runID++;
                    Logger.warning(source.getName() + " --patch--> " + target.getName());
                    final Path pathToTarget = variantsDirV0.path().resolve(target.getName());
                    final Path pathToExpectedResult = variantsDirV1.path().resolve(target.getName());
                    final FineDiff evolutionDiff = getFineDiff(getOriginalDiff(pathToTarget, pathToExpectedResult));
                    if (inDebug) {
                        saveDiff(evolutionDiff, debugDir.resolve("evolutionDiff.txt"));
                    }

                    /* Application of patches without knowledge about features */
                    Logger.info("Applying patch without knowledge about features...");
                    // Apply the fine diff to the target variant
                    final List<Path> skippedNormal = applyPatch(normalPatchFile, pathToTarget, rejectsNormalFile);
                    // Evaluate the patch result
                    final FineDiff actualVsExpectedNormal = getActualVsExpected(pathToExpectedResult);
                    final OriginalDiff rejectsNormal = readRejects(rejectsNormalFile);

                    /* Application of patches with knowledge about PC of edit only */
                    Logger.info("Applying patch with knowledge about edits' PCs...");
                    // Create target variant specific patch that respects PCs
                    final FineDiff filteredPatch = getFilteredDiff(originalDiff, groundTruthV0.get(source).artefact(), groundTruthV1.get(source).artefact(), target, variantsDirV0.path(), variantsDirV1.path());
                    final boolean emptyPatch = filteredPatch.content().isEmpty();
                    saveDiff(filteredPatch, filteredPatchFile);
                    // Apply the patch
                    applyPatch(filteredPatchFile, pathToTarget, rejectsFilteredFile, emptyPatch);
                    // Evaluate the result
                    final FineDiff actualVsExpectedFiltered = getActualVsExpected(pathToExpectedResult);
                    final OriginalDiff rejectsFiltered = readRejects(rejectsFilteredFile);

                    /* Result Evaluation */
                    final PatchOutcome patchOutcome = ResultAnalysis.processOutcome(
                            experimentalSubject.name(),
                            runID,
                            source.getName(),
                            target.getName(),
                            parentCommit, childCommit,
                            normalPatch, filteredPatch,
                            actualVsExpectedNormal, actualVsExpectedFiltered,
                            rejectsNormal, rejectsFiltered,
                            evolutionDiff,
                            skippedNormal);

                    try {
                        patchOutcome.writeAsJSON(resultFile, true);
                    } catch (final IOException e) {
                        Logger.error("Was not able to write filtered patch result file for run " + runID, e);
                    }

                    Logger.info("Finished patching for source " + source.getName() + " and target " + target.getName());
                }
            }
            pairCount++;
            Logger.warning(String.format("Finished commit %d of %d.%n", pairCount, historySize));
            Logger.status(String.format("Cleaning data of commit %s", parentCommit.id()));
            shell.execute(new RmCommand(parentCommit.getCommitDataDirectory()).recursive());
            Logger.status(String.format("Cleaning data of commit %s", childCommit.id()));
            shell.execute(new RmCommand(childCommit.getCommitDataDirectory()).recursive());
        }
        Logger.status("All done.");
    }

    /**
     * Get the difference between the target variant after patching and the target variant in the next evolution step.
     * Then, filter all differences that do not belong to the source variant and could have therefore not been synchronized
     * in any case.
     */
    private FineDiff getActualVsExpected(final Path pathToExpectedResult) {
        final OriginalDiff resultDiff = getOriginalDiff(patchDir, pathToExpectedResult);
        if (inDebug) {
            try {
                Files.write(debugDir.resolve("resultDiffOriginal.txt"), resultDiff.toLines());
            } catch (final IOException e) {
                Logger.error("Was not able to save resultDiffOriginal", e);
            }
        }
        FineDiff fineResult = getFineDiff(resultDiff);
        if (inDebug) {
            try {
                Files.write(debugDir.resolve("resultDiffFine.txt"), fineResult.toLines());
            } catch (final IOException e) {
                Logger.error("Was not able to save resultDiffFiltered", e);
            }
        }

        return fineResult;
    }

    protected abstract Sample sample(SPLCommit parentCommit, SPLCommit childCommit);

    private void generateVariant(final SPLCommit parentCommit,
                                 final SPLCommit childCommit,
                                 final Map<Variant, GroundTruth> groundTruthV0,
                                 final Map<Variant, GroundTruth> groundTruthV1,
                                 final Variant variant,
                                 final SimpleFileFilter filter) {
        Logger.status("Generating variant " + variant.getName());
        if (inDebug && variant.getConfiguration() instanceof FeatureIDEConfiguration config) {
            try {
                Files.write(debugDir.resolve(variant.getName() + ".config"), config.toAssignment().entrySet().stream().map(entry -> entry.getKey() + " : " + entry.getValue()).collect(Collectors.toList()));
            } catch (final IOException e) {
                Logger.error("Was not able to write configuration of " + variant.getName(), e);
            }
        }

        try {
            Files.createDirectories(variantsDirV0.path().resolve(variant.getName()));
            Files.createDirectories(variantsDirV1.path().resolve(variant.getName()));
        } catch (final IOException e) {
            e.printStackTrace();
            panic("Was not able to create directory for variant: " + variant.getName());
        }

        final GroundTruth gtV0 = parentCommit
                .presenceConditions()
                .run()
                .orElseThrow()
                .generateVariant(
                        variant,
                        new CaseSensitivePath(splCopyA),
                        variantsDirV0.resolve(variant.getName()),
                        VariantGenerationOptions.ExitOnErrorButAllowNonExistentFiles(filter))
                .expect("Was not able to generate V0 of " + variant);
        if (inDebug) {
            try {
                Resources.Instance().write(Artefact.class, gtV0.artefact(), debugDir.resolve("V0-" + variant.getName() + ".variant.csv"));
            } catch (final Resources.ResourceIOException e) {
                Logger.error("Was not able to write ground truth.");
            }
        }
        groundTruthV0.put(variant, gtV0);

        final GroundTruth gtV1 = childCommit
                .presenceConditions()
                .run()
                .orElseThrow()
                .generateVariant(
                        variant,
                        new CaseSensitivePath(splCopyB),
                        variantsDirV1.resolve(variant.getName()),
                        VariantGenerationOptions.ExitOnErrorButAllowNonExistentFiles(filter))
                .expect("Was not able to generate V1 of " + variant);
        if (inDebug) {
            try {
                Resources.Instance().write(Artefact.class, gtV1.artefact(), debugDir.resolve("V1-" + variant.getName() + ".variant.csv"));
            } catch (final Resources.ResourceIOException e) {
                Logger.error("Was not able to write ground truth.", e);
            }
        }
        groundTruthV1.put(variant, gtV1);
    }

    protected SimpleFileFilter splRepoPreparation(final SPLRepository parentRepo, final SPLRepository childRepo, final SPLCommit parentCommit, final SPLCommit childCommit) {
        Logger.info("Next V0 commit: " + parentCommit);
        Logger.info("Next V1 commit: " + childCommit);
        // Checkout the commits in the SPL repository
        try {
            preprocessSPLRepositories(parentRepo, childRepo);

            Logger.status("Checkout of commits in SPL repo.");
            parentRepo.checkoutCommit(parentCommit, true);
            childRepo.checkoutCommit(childCommit, true);

        } catch (final GitAPIException | IOException e) {
            panic("Was not able to checkout commit for SPL repository.", e);
        }
        Logger.info("Done.");

        Logger.info("Diffing SPL commits for find changed files.");
        final OriginalDiff diff = getOriginalDiff(splCopyA, splCopyB);
        final Set<Path> filesToKeep = new HashSet<>();
        for (final FileDiff fileDiff : diff.fileDiffs()) {
            if (!fileDiff.oldFile().getName(1).startsWith(".git")) {
                filesToKeep.add(fileDiff.oldFile().subpath(1, fileDiff.oldFile().getNameCount()));
            }
        }

        postprocessSPLRepositories(parentRepo, childRepo);

        return new SimpleFileFilter(filesToKeep);
    }

    protected abstract void postprocessSPLRepositories(SPLRepository parentRepo, SPLRepository childRepo);

    protected abstract void preprocessSPLRepositories(SPLRepository parentRepo, SPLRepository childRepo);


    private VariabilityDataset init() {
        Logger.status("Starting experiment initialization.");
        Logger.setLogLevel(logLevel);
        // Clean old SPL repo files
        Logger.status("Cleaning old repo files.");
        if (Files.exists(splCopyA)) {
            shell.execute(new RmCommand(splCopyA).recursive()).expect("Was not able to remove SPL-V0.");
        }
        if (Files.exists(splCopyB)) {
            shell.execute(new RmCommand(splCopyB).recursive()).expect("Was not able to remove SPL-V1.");
        }
        // Copy the SPL repo
        Logger.status("Creating new SPL repo copies.");
        shell.execute(new CpCommand(splRepositoryPath, splCopyA).recursive()).expect("Was not able to copy SPL-V0.");
        shell.execute(new CpCommand(splRepositoryPath, splCopyB).recursive()).expect("Was not able to copy SPL-V1.");


        // Load VariabilityDataset
        Logger.status("Loading variability dataset.");
        VariabilityDataset dataset = null;
        try {
            final Resources instance = Resources.Instance();
            final VariabilityDatasetLoader datasetLoader = new VariabilityDatasetLoader();
            instance.registerLoader(VariabilityDataset.class, datasetLoader);
            dataset = instance.load(VariabilityDataset.class, datasetPath);
            Logger.status("Dataset loaded.");
        } catch (final Resources.ResourceIOException e) {
            panic("Was not able to load dataset.", e);
        }

        // Retrieve pairs/sequences of usable commits
        Logger.status("Retrieving commit pairs");
        return dataset;
    }

    private void saveDiff(final FineDiff fineDiff, final Path file) {
        // Save the fine diff to a file
        try {
            Files.write(file, fineDiff.toLines());
        } catch (final IOException e) {
            panic("Was not able to save diff to file " + file);
        }
    }

    private List<Path> applyPatch(final Path patchFile, final Path targetVariant, final Path rejectFile) {
        return applyPatch(patchFile, targetVariant, rejectFile, false);
    }

    private List<Path> applyPatch(final Path patchFile, final Path targetVariant, final Path rejectFile, final boolean emptyPatch) {
        // Clean patch directory
        if (Files.exists(patchDir.toAbsolutePath())) {
            shell.execute(new RmCommand(patchDir.toAbsolutePath()).recursive());
        }

        try {
            Files.createDirectories(patchDir.getParent());
        } catch (IOException e) {
            e.printStackTrace();
            panic("Was not able to create patch directories: ", e);
        }

        if (Files.exists(rejectFile)) {
            Logger.info("Cleaning old rejects file " + rejectFile);
            shell.execute(new RmCommand(rejectFile));
        }

        // copy target variant
        shell.execute(new CpCommand(targetVariant, patchDir).recursive()).expect("Was not able to copy variant " + targetVariant);

        // apply patch to copied target variant
        final List<Path> skipped = new LinkedList<>();
        if (!emptyPatch) {
            final Result<List<String>, ShellException> result = shell.execute(PatchCommand.Recommended(patchFile).strip(2).rejectFile(rejectFile).force(), patchDir);
            if (result.isSuccess()) {
                result.getSuccess().forEach(Logger::info);
            } else {
                final List<String> lines = result.getFailure().getOutput();
                Logger.info("Failed to apply part of patch. See debug log and rejects file for more information");
                String oldFile;
                for (final String nextLine : lines) {
                    Logger.debug(nextLine);
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
    private FineDiff getFineDiff(final OriginalDiff originalDiff) {
        final DefaultContextProvider contextProvider = new DefaultContextProvider(workDir);
        return DiffSplitter.split(originalDiff, contextProvider);
    }

    private FineDiff getFilteredDiff(final OriginalDiff originalDiff, final Artefact tracesV0, final Artefact tracesV1, final Variant target, Path oldVersionRoot, Path newVersionRoot) {
        final EditFilter editFilter = new EditFilter(tracesV0, tracesV1, target, oldVersionRoot, newVersionRoot, 2);
        return getFilteredDiff(originalDiff, editFilter);
    }

    private <T extends IFileDiffFilter & ILineFilter> FineDiff getFilteredDiff(final OriginalDiff originalDiff, final T filter) {
        // Create target variant specific patch that respects PCs
        final IContextProvider contextProvider = new DefaultContextProvider(workDir);
        return DiffSplitter.split(originalDiff, filter, filter, contextProvider);
    }

    protected OriginalDiff getOriginalDiff(final Path v0Path, final Path v1Path) {
        final DiffCommand diffCommand = DiffCommand.Recommended(workDir.relativize(v0Path), workDir.relativize(v1Path));
        final List<String> output = shell.execute(diffCommand, workDir).expect("Was not able to diff variants.");
        return DiffParser.toOriginalDiff(output);
    }

    protected void panic(final String message) {
        Logger.error(message);
        throw new Panic(message);
    }

    protected void panic(final String message, final Exception e) {
        Logger.error(message, e);
        e.printStackTrace();
        throw new Panic(message);
    }


}
