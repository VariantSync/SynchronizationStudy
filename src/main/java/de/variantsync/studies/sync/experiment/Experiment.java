package de.variantsync.studies.sync.experiment;

import de.variantsync.evolution.feature.Variant;
import de.variantsync.evolution.feature.config.FeatureIDEConfiguration;
import de.variantsync.evolution.feature.sampling.*;
import de.variantsync.evolution.io.Resources;
import de.variantsync.evolution.io.data.VariabilityDatasetLoader;
import de.variantsync.evolution.repository.SPLRepository;
import de.variantsync.evolution.util.LogLevel;
import de.variantsync.evolution.util.Logger;
import de.variantsync.evolution.util.functional.Result;
import de.variantsync.evolution.util.io.CaseSensitivePath;
import de.variantsync.evolution.variability.CommitPair;
import de.variantsync.evolution.variability.SPLCommit;
import de.variantsync.evolution.variability.VariabilityDataset;
import de.variantsync.evolution.variability.pc.Artefact;
import de.variantsync.evolution.variability.pc.options.VariantGenerationOptions;
import de.variantsync.evolution.variability.pc.groundtruth.GroundTruth;
import de.variantsync.studies.sync.diff.DiffParser;
import de.variantsync.studies.sync.diff.components.FileDiff;
import de.variantsync.studies.sync.diff.components.FineDiff;
import de.variantsync.studies.sync.diff.components.OriginalDiff;
import de.variantsync.studies.sync.diff.filter.EditFilter;
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
    // TODO: Set in external config
    protected final Path workDir;
    protected final Path debugDir;
    protected final Path resultFile;
    protected final Path splRepositoryV0Path;
    protected final Path splRepositoryV1Path;
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
    protected final Set<CommitPair<SPLCommit>> pairs;

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
        resultFile = mainDir.resolve("results.txt");
        splRepositoryPath = Path.of(config.EXPERIMENT_DIR_SPL());
        datasetPath = Path.of(config.EXPERIMENT_DIR_DATASET());
        splRepositoryV0Path = workDir.resolve("SPL-V0");
        splRepositoryV1Path = workDir.resolve("SPL-V1");
        variantsDirV0 = new CaseSensitivePath(workDir.resolve("V0Variants"));
        variantsDirV1 = new CaseSensitivePath(workDir.resolve("V1Variants"));
        patchDir = workDir.resolve("TARGET");
        normalPatchFile = workDir.resolve("patch.txt");
        filteredPatchFile = workDir.resolve("filtered-patch.txt");
        rejectsNormalFile = workDir.resolve("rejects-normal.txt");
        rejectsFilteredFile = workDir.resolve("rejects-filtered.txt");
        shell = new ShellExecutor(Logger::debug, Logger::error, workDir);
        logLevel = config.EXPERIMENT_LOGGER_LEVEL();
        randomRepeats = config.EXPERIMENT_REPEATS();
        numVariants = config.EXPERIMENT_VARIANT_COUNT();
        experimentalSubject = config.EXPERIMENT_SUBJECT();

        pairs = init();
    }


    public void run() {
        final Random random = new Random();

        // Initialize the SPL repositories for different versions
        Logger.status("Initializing SPL repos.");
        final SPLRepository splRepositoryV0 = new SPLRepository(splRepositoryV0Path);
        final SPLRepository splRepositoryV1 = new SPLRepository(splRepositoryV1Path);

        // For each pair
        Logger.status("Starting diffing and patching...");
        long runID = 0;
        int pairCount = 0;
        Logger.status("There are " + pairs.size() + " pairs to work on.");
        for (final CommitPair<SPLCommit> pair : pairs) {
            // Take next commit pair
            final SPLCommit commitV0 = pair.parent();
            final SPLCommit commitV1 = pair.child();

            final SimpleFileFilter fileFilter = splRepoPreparation(splRepositoryV0, splRepositoryV1, commitV0, commitV1);

            // While more random configurations to consider
            for (int i = 0; i < randomRepeats; i++) {
                Logger.warning("Starting repetition " + (i + 1) + " of " + randomRepeats + " with " + numVariants + " variants.");
                // Sample set of random variants
                Logger.status("Sampling next set of variants...");
                final Sample sample = sample(commitV0, commitV1);
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
                try {
                    Resources.Instance().write(Artefact.class, commitV0.presenceConditions().run().get(), debugDir.resolve("V0.spl.csv"));
                    Resources.Instance().write(Artefact.class, commitV1.presenceConditions().run().get(), debugDir.resolve("V1.spl.csv"));
                } catch (final Resources.ResourceIOException e) {
                    panic("Was not able to write PCs", e);
                }

                // Generate the randomly selected variants at both versions
                final Map<Variant, GroundTruth> groundTruthV0 = new HashMap<>();
                final Map<Variant, GroundTruth> groundTruthV1 = new HashMap<>();
                Logger.status("Generating variants...");
                for (final Variant variant : sample.variants()) {
                    generateVariant(commitV0, commitV1, groundTruthV0, groundTruthV1, variant, fileFilter);
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
                    Logger.status("Skipping " + source + " as diff source. Diff is empty");
                    continue;
                } else {
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
                    Logger.status("Considering variant " + target.getName() + " as next target.");
                    final Path pathToTarget = variantsDirV0.path().resolve(target.getName());
                    final Path pathToExpectedResult = variantsDirV1.path().resolve(target.getName());

                    /* Application of patches without knowledge about features */
                    Logger.info("Applying patch without knowledge about features...");
                    // Apply the fine diff to the target variant
                    final List<Path> skippedNormal = applyPatch(normalPatchFile, pathToTarget, rejectsNormalFile);
                    // Evaluate the patch result
                    final OriginalDiff actualVsExpectedNormal = getOriginalDiff(patchDir, pathToExpectedResult);
                    final OriginalDiff rejectsNormal = readRejects(rejectsNormalFile);

                    /* Application of patches with knowledge about PC of edit only */
                    Logger.info("Applying patch with knowledge about edits' PCs...");
                    // Create target variant specific patch that respects PCs
                    final FineDiff filteredPatch = getFilteredDiff(originalDiff, groundTruthV0.get(source).artefact(), groundTruthV1.get(source).artefact(), target);
                    final boolean emptyPatch = filteredPatch.content().isEmpty();
                    saveDiff(filteredPatch, filteredPatchFile);
                    // Apply the patch
                    final List<Path> skippedFiltered = applyPatch(filteredPatchFile, pathToTarget, rejectsFilteredFile, emptyPatch);
                    assert skippedFiltered.isEmpty();
                    // Evaluate the result
                    final OriginalDiff actualVsExpectedFiltered = getOriginalDiff(patchDir, pathToExpectedResult);
                    final OriginalDiff rejectsFiltered = readRejects(rejectsFilteredFile);

                    /* Result Evaluation */
                    final PatchOutcome patchOutcome = ResultAnalysis.processOutcome(
                            experimentalSubject.name(),
                            runID,
                            source.getName(),
                            target.getName(),
                            commitV0, commitV1,
                            normalPatch, filteredPatch,
                            actualVsExpectedNormal, actualVsExpectedFiltered,
                            rejectsNormal, rejectsFiltered,
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
            Logger.status(String.format("Finished commit pair %d of %d.%n", pairCount, pairs.size()));
            Logger.status(String.format("Cleaning data of commit %s", commitV0.id()));
            shell.execute(new RmCommand(commitV0.getCommitDataDirectory()).recursive());
            Logger.status(String.format("Cleaning data of commit %s", commitV1.id()));
            shell.execute(new RmCommand(commitV1.getCommitDataDirectory()).recursive());
        }
        Logger.status("All done.");
    }

    protected abstract Sample sample(SPLCommit commitV0, SPLCommit commitV1);

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

    private void generateVariant(final SPLCommit commitV0,
                                 final SPLCommit commitV1,
                                 final Map<Variant, GroundTruth> groundTruthV0,
                                 final Map<Variant, GroundTruth> groundTruthV1,
                                 final Variant variant,
                                 final SimpleFileFilter filter) {
        Logger.status("Generating variant " + variant.getName());
        if (variant.getConfiguration() instanceof FeatureIDEConfiguration config) {
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

        final GroundTruth gtV0 = commitV0
                .presenceConditions()
                .run()
                .orElseThrow()
                .generateVariant(
                        variant,
                        new CaseSensitivePath(splRepositoryV0Path),
                        variantsDirV0.resolve(variant.getName()),
                        VariantGenerationOptions.ExitOnErrorButAllowNonExistentFiles(filter))
                .expect("Was not able to generate V0 of " + variant);
        try {
            Resources.Instance().write(Artefact.class, gtV0.artefact(), debugDir.resolve("V0-" + variant.getName() + ".variant.csv"));
        } catch (final Resources.ResourceIOException e) {
            Logger.error("Was not able to write ground truth.");
        }
        groundTruthV0.put(variant, gtV0);

        final GroundTruth gtV1 = commitV1
                .presenceConditions()
                .run()
                .orElseThrow()
                .generateVariant(
                        variant,
                        new CaseSensitivePath(splRepositoryV1Path),
                        variantsDirV1.resolve(variant.getName()),
                        VariantGenerationOptions.ExitOnErrorButAllowNonExistentFiles(filter))
                .expect("Was not able to generate V1 of " + variant);
        try {
            Resources.Instance().write(Artefact.class, gtV1.artefact(), debugDir.resolve("V1-" + variant.getName() + ".variant.csv"));
        } catch (final Resources.ResourceIOException e) {
            Logger.error("Was not able to write ground truth.", e);
        }
        groundTruthV1.put(variant, gtV1);
    }

    private SimpleFileFilter splRepoPreparation(final SPLRepository splRepositoryV0, final SPLRepository splRepositoryV1, final SPLCommit commitV0, final SPLCommit commitV1) {
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

        } catch (final GitAPIException | IOException e) {
            panic("Was not able to checkout commit for SPL repository.", e);
        }
        Logger.info("Done.");

        Logger.info("Diffing SPL commits for find changed files.");
        final OriginalDiff diff = getOriginalDiff(splRepositoryV0Path, splRepositoryV1Path);
        final Set<Path> filesToKeep = new HashSet<>();
        for (final FileDiff fileDiff : diff.fileDiffs()) {
            filesToKeep.add(fileDiff.oldFile().subpath(1,fileDiff.oldFile().getNameCount()));
        }

        if (experimentalSubject == EExperimentalSubject.BUSYBOX) {
            Logger.status("Normalizing BusyBox files...");
            try {
                BusyboxPreparation.normalizeDir(splRepositoryV0Path.toFile());
                BusyboxPreparation.normalizeDir(splRepositoryV1Path.toFile());
            } catch (final IOException e) {
                Logger.error("", e);
                panic("Was not able to normalize BusyBox.", e);
            }
        }

        return new SimpleFileFilter(filesToKeep);
    }

    private Set<CommitPair<SPLCommit>> init() {
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
        return Objects.requireNonNull(dataset).getCommitPairsForEvolutionStudy();
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
                Logger.error("Failed to apply part of patch.");
                String oldFile;
                for (final String nextLine : lines) {
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

    private FineDiff getFilteredDiff(final OriginalDiff originalDiff, final Artefact tracesV0, final Artefact tracesV1, final Variant target) {
        final EditFilter editFilter = new EditFilter(tracesV0, tracesV1, target, variantsDirV0.path(), variantsDirV1.path(), 2);
        // Create target variant specific patch that respects PCs
        final IContextProvider contextProvider = new DefaultContextProvider(workDir);
        return DiffSplitter.split(originalDiff, editFilter, editFilter, contextProvider);
    }

    private OriginalDiff getOriginalDiff(final Path v0Path, final Path v1Path) {
        final DiffCommand diffCommand = DiffCommand.Recommended(workDir.relativize(v0Path), workDir.relativize(v1Path));
        final List<String> output = shell.execute(diffCommand, workDir).expect("Was not able to diff variants.");
        return DiffParser.toOriginalDiff(output);
    }

    private void panic(final String message) {
        Logger.error(message);
        throw new Panic(message);
    }

    private void panic(final String message, final Exception e) {
        Logger.error(message, e);
        e.printStackTrace();
        throw new Panic(message);
    }


}
