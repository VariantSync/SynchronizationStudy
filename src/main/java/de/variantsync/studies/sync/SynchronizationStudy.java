package de.variantsync.studies.sync;

import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.variantsync.evolution.feature.Sample;
import de.variantsync.evolution.feature.Variant;
import de.variantsync.evolution.feature.sampling.FeatureIDESampler;
import de.variantsync.evolution.io.Resources;
import de.variantsync.evolution.io.data.VariabilityDatasetLoader;
import de.variantsync.evolution.repository.SPLRepository;
import de.variantsync.evolution.util.CaseSensitivePath;
import de.variantsync.evolution.util.Logger;
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
import de.variantsync.studies.sync.diff.filter.PCBasedFilter;
import de.variantsync.studies.sync.diff.splitting.DefaultContextProvider;
import de.variantsync.studies.sync.diff.splitting.DiffSplitter;
import de.variantsync.studies.sync.error.Panic;
import de.variantsync.studies.sync.error.ShellException;
import de.variantsync.studies.sync.shell.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.NotNull;
import org.prop4j.Node;

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
    private static final int numVariants = 3;
    private static final SPLRepository splRepositoryV0 = new SPLRepository(workDir.getParent().resolve("busybox-V0"));
    private static final SPLRepository splRepositoryV1 = new SPLRepository(workDir.getParent().resolve("busybox-V1"));
    private static final CaseSensitivePath variantsDirV0 = new CaseSensitivePath(workDir.resolve("V0Variants"));
    private static final CaseSensitivePath variantsDirV1 = new CaseSensitivePath(workDir.resolve("V1Variants"));
    private static final Path patchDir = workDir.resolve("TARGET");
    private static final Path normalPatchFile = workDir.resolve("patch.txt");
    private static final Path pcBasedPatchFile = workDir.resolve("pc-patch.txt");
    private static final Path rejectFile = workDir.resolve("rejects.txt");
    private static final FeatureIDESampler variantSampler = FeatureIDESampler.CreateRandomSampler(numVariants);
    private static final ShellExecutor shell = new ShellExecutor(Logger::debug, Logger::error, workDir);


    public static void main(String... args) {
        // Initialize the library
        de.variantsync.evolution.Main.Initialize();
        
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
        Set<CommitPair<SPLCommit>> pairs = Objects.requireNonNull(dataset).getCommitPairsForEvolutionStudy();
//        var history = dataset.getVariabilityHistory(setOfCommits -> {
//            List<NonEmptyList<SPLCommit>> sequences = new ArrayList<>(pairs.size());
//            pairs.stream().map(p -> {
//                List<SPLCommit> list = new ArrayList<>(2);
//                list.add(p.parent());
//                list.add(p.child());
//                return new NonEmptyList<>(list);
//            }).forEach(sequences::add);
//            return sequences;
//        });

        // For each pair
        Logger.status("Starting diffing and patching...");
        for (CommitPair<SPLCommit> pair : pairs) {
            // Take next commit pair
            SPLCommit commitV0 = pair.parent();
            SPLCommit commitV1 = pair.child();

            Logger.info("Next V0 commit: " + commitV0);
            Logger.info("Next V1 commit: " + commitV1);
            Logger.info("Checkout of commits in SPL repo...");
            // Checkout the commits in the SPL repository
            try {
                splRepositoryV0.checkoutCommit(commitV0);
                splRepositoryV1.checkoutCommit(commitV1);
            } catch (GitAPIException | IOException e) {
                panic("Was not able to checkout commit for SPL repository.", e);
            }
            Logger.info("Done.");

            IFeatureModel modelV0 = commitV0.featureModel().run().orElseThrow();
            IFeatureModel modelV1 = commitV1.featureModel().run().orElseThrow();
            final Node featureModelFormulaV0 = new FeatureModelFormula(modelV0).getPropositionalNode();
            final Node featureModelFormulaV1 = new FeatureModelFormula(modelV0).getPropositionalNode();
            
            // While more random configurations to consider
            for (int i = 0; i < randomRepeats; i++) {
                Logger.status("Starting repetition " + (i+1) + " of " + randomRepeats + " with (random) variants.");
                // Sample set of random variants
                Logger.status("Sampling next set of variants...");
                Sample configurations = variantSampler.sample(modelV0);
                Logger.status("Done. Sampled " + configurations.variants().size() + " variants.");

                if (Files.exists(debugDir)) {
                    shell.execute(new RmCommand(debugDir).recursive());
                }
                if(debugDir.toFile().mkdirs()) {
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
                
                // Generate the randomly selected variants at both versions
                Map<Variant, GroundTruth> groundTruthV0 = new HashMap<>();
                Map<Variant, GroundTruth> groundTruthV1 = new HashMap<>();
                Logger.status("Generating variants...");
                for (Variant variant : configurations.variants()) {
                    Logger.status("Generating variant " + variant.getName());
                    // TODO: Check whether this is enough. It only checks satisfiability, but might not check whether the variables exist in the feature model. Maybe check whether the set of selected variables exists?
                    if (!variant.isImplementing(featureModelFormulaV0)) {
                        panic("Sampled " + variant + " is not valid for feature model " + modelV0 + "!");
                    }
                    if (!variant.isImplementing(featureModelFormulaV1)) {
                        panic("Sampled " + variant + " is not valid for feature model " + modelV1 + "!");
                    }
                    // TODO: Retrieve ground truth
                    GroundTruth gtV0 = commitV0.presenceConditions().run().orElseThrow().generateVariant(variant, new CaseSensitivePath(splRepositoryV0.getPath()), variantsDirV0.resolve(variant.getName()), VariantGenerationOptions.ExitOnError).expect("Was not able to generate V0 of " + variant);
                    try {
                        Resources.Instance().write(Artefact.class, gtV0.artefact(), debugDir.resolve("V0-" + variant.getName() + ".variant.csv"));
                    } catch (Resources.ResourceIOException e) {
                        Logger.error("Was not able to write ground truth.");
                    }
                    groundTruthV0.put(variant, gtV0);
                    
                    GroundTruth gtV1 = commitV1.presenceConditions().run().orElseThrow().generateVariant(variant, new CaseSensitivePath(splRepositoryV1.getPath()), variantsDirV1.resolve(variant.getName()), VariantGenerationOptions.ExitOnError).expect("Was not able to generate V1 of " + variant);
                    try {
                        Resources.Instance().write(Artefact.class, gtV1.artefact(), debugDir.resolve("V1-" + variant.getName() + ".variant.csv"));
                    } catch (Resources.ResourceIOException e) {
                        Logger.error("Was not able to write ground truth.", e);
                    }
                    groundTruthV1.put(variant, gtV1);
                }
                Logger.status("Done.");

                // Select next source variant
                for (Variant source : configurations.variants()) {
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
                    }
                    Logger.info("Converting diff...");
                    // Convert the original diff into a fine diff
                    FineDiff fineDiff = getFineDiff(originalDiff);
                    saveDiff(fineDiff, normalPatchFile);
                    Logger.info("Saved fine diff.");

                    // For each target variant,
                    Logger.status("Starting patch application for source variant " + source.getName());
                    for (Variant target : configurations.variants()) {
                        if (target == source) {
                            continue;
                        }
                        Logger.info("Considering variant " + target.getName() + " as next target.");
                        if (Files.exists(pcBasedPatchFile)) {
                            Logger.info("Cleaning old PC-based patch file " + pcBasedPatchFile);
                            shell.execute(new RmCommand(pcBasedPatchFile));
                        }
                        Path pathToTarget = variantsDirV0.path().resolve(target.getName());
                        Path pathToExpectedResult = variantsDirV1.path().resolve(target.getName());

                        /* Application of patches without knowledge about features */
                        Logger.info("Applying patch without knowledge about features...");
                        // Apply the fine diff to the target variant
                        applyPatch(normalPatchFile, pathToTarget);
                        // Evaluate the patch result
                        evaluatePatchResult(pathToExpectedResult, fineDiff);
                        
                        /* Application of patches with knowledge about features */
                        Logger.info("Applying patch with knowledge about features...");
                        // Create target variant specific patch that respects PCs
                        FineDiff pcBasedDiff = getPCBasedDiff(originalDiff, groundTruthV0.get(source).artefact(), groundTruthV1.get(source).artefact(), target);
                        saveDiff(pcBasedDiff, pcBasedPatchFile);
                        // Apply the patch
                        applyPatch(pcBasedPatchFile, pathToTarget);
                        // Evaluate the result
                        evaluatePatchResult(pathToExpectedResult, pcBasedDiff);
                        
                        Logger.info("Finished patching for source " + source.getName() + " and target " + target.getName());
                    }
                }
            }
        }
    }

    private static void evaluatePatchRejects(FineDiff appliedPatch) {
        int commitSized = appliedPatch.content().isEmpty() ? 0 : 1;
        int fileSized = new HashSet<>(appliedPatch.content().stream().map(fd -> fd.oldFile().toString()).collect(Collectors.toList())).size();
        int editSized = appliedPatch.content().stream().mapToInt(fd -> fd.hunks().size()).sum();
        
        if (Files.exists(rejectFile)) {
            try {
                List<String> rejects = Files.readAllLines(rejectFile);
                OriginalDiff rejectsDiff = DiffParser.toOriginalDiff(rejects);
                Logger.status("Commit-sized patch failed");
                commitSized -= 1;
                
                int fileSizedFail = new HashSet<>(rejectsDiff.fileDiffs().stream().map(fd -> fd.oldFile().toString()).collect(Collectors.toList())).size();
                fileSized -= fileSizedFail;
                Logger.status("" + fileSizedFail + " file-sized patches failed.");
                int editSizedFail = rejectsDiff.fileDiffs().stream().mapToInt(fd -> fd.hunks().size()).sum();
                editSized -= editSizedFail;
                Logger.status("" + editSizedFail + " edit-sized patches failed");
            } catch (IOException e) {
                Logger.error("Was not able to read rejects file.", e);
            }
        } 
        Logger.status(commitSized + " commit-sized patches successful.");
        Logger.status(fileSized + " file-sized patches successful.");
        Logger.status(editSized + " edit-sized patches successful.");
        
        // TODO: Implement evaluation
        Logger.error("TODO");
    }

    private static void saveDiff(FineDiff fineDiff, Path file) {
        // Save the fine diff to a file
        try {
            Files.write(file, fineDiff.toLines());
        } catch (IOException e) {
            panic("Was not able to save diff to file " + file);
        }
    }

    private static void applyPatch(Path patchFile, Path targetVariant) {
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

        /* Application of patches without knowledge about features */
        // apply patch to copied target variant
        Result<List<String>, ShellException> result = shell.execute(PatchCommand.Recommended(patchFile).strip(2).rejectFile(rejectFile), patchDir);
        if (result.isSuccess()) {
            result.getSuccess().forEach(Logger::info);
        } else {
            Logger.error("Failed to apply patch.", result.getFailure());
        }
    }

    private static void evaluatePatchResult(Path targetVariant, FineDiff patch) {
        // diff patch result and target variant
        DiffCommand diffCommand = DiffCommand.Recommended(targetVariant, patchDir);
        List<String> output = shell.execute(diffCommand).expect("Was not able to diff variants.");
        // evaluate diff
        if (output.isEmpty()) {
            // TODO
            Logger.info("No difference between patched variant and actual target");
        } else {
            // TODO
            Logger.warning("There are differences between the patched variant and the actual target");
        }
        
        // evaluate patch rejects
        evaluatePatchRejects(patch);
        
        // TODO: cleanup rejects etc.
    }

    @NotNull
    private static FineDiff getFineDiff(OriginalDiff originalDiff) {
        DefaultContextProvider contextProvider = new DefaultContextProvider(workDir);
        return DiffSplitter.split(originalDiff, contextProvider);
    }

    private static FineDiff getPCBasedDiff(OriginalDiff originalDiff, Artefact tracesV0, Artefact tracesV1, Variant target) {
        PCBasedFilter pcBasedFilter = new PCBasedFilter(tracesV0, tracesV1, target, variantsDirV0.path(), variantsDirV1.path(), 2);
        // Create target variant specific patch that respects PCs
        DefaultContextProvider contextProvider = new DefaultContextProvider(workDir);
        return DiffSplitter.split(originalDiff, pcBasedFilter, pcBasedFilter, contextProvider);
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
        throw new Panic(message);
    }


}