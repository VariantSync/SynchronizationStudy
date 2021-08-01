package de.variantsync.studies.sync;

import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.variantsync.evolution.feature.Sample;
import de.variantsync.evolution.feature.Variant;
import de.variantsync.evolution.feature.sampling.FeatureIDESampler;
import de.variantsync.evolution.io.Resources;
import de.variantsync.evolution.repository.SPLRepository;
import de.variantsync.evolution.util.CaseSensitivePath;
import de.variantsync.evolution.util.Logger;
import de.variantsync.evolution.variability.CommitPair;
import de.variantsync.evolution.variability.SPLCommit;
import de.variantsync.evolution.variability.VariabilityDataset;
import de.variantsync.evolution.variability.pc.Artefact;
import de.variantsync.evolution.variability.pc.VariantGenerationOptions;
import de.variantsync.studies.sync.diff.DiffParser;
import de.variantsync.studies.sync.diff.components.FineDiff;
import de.variantsync.studies.sync.diff.components.OriginalDiff;
import de.variantsync.studies.sync.diff.filter.PCBasedFilter;
import de.variantsync.studies.sync.diff.splitting.DefaultContextProvider;
import de.variantsync.studies.sync.diff.splitting.DiffSplitter;
import de.variantsync.studies.sync.error.Panic;
import de.variantsync.studies.sync.shell.CpCommand;
import de.variantsync.studies.sync.shell.DiffCommand;
import de.variantsync.studies.sync.shell.PatchCommand;
import de.variantsync.studies.sync.shell.ShellExecutor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.NotNull;
import org.prop4j.Node;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class SynchronizationStudy {
    // TODO: Set in external config
    private static final Path datasetPath = Path.of("");
    private static final Path workDir = Path.of("");
    private static final int randomRepeats = 1;
    private static final SPLRepository splRepositoryV0 = new SPLRepository(null);
    private static final SPLRepository splRepositoryV1 = new SPLRepository(null);
    private static final CaseSensitivePath variantsDirV0 = new CaseSensitivePath(workDir.resolve("V0Variants"));
    private static final CaseSensitivePath variantsDirV1 = new CaseSensitivePath(workDir.resolve("V1Variants"));
    private static final Path patchDir = workDir.resolve("TARGET");
    private static final Path normalPatchFile = workDir.resolve("patch.txt");
    private static final Path pcBasedPatchFile = workDir.resolve("pc-patch.txt");
    private static final FeatureIDESampler variantSampler = FeatureIDESampler.CreateRandomSampler(randomRepeats);
    private static final ShellExecutor shell = new ShellExecutor(Logger::info, Logger::error, workDir);


    public static void main(String... args) {
        // Load VariabilityDataset
        VariabilityDataset dataset = null;
        try {
            dataset = Resources.Instance().load(VariabilityDataset.class, datasetPath);
        } catch (Resources.ResourceIOException e) {
            panic("Was not able to load dataset.");
        }

        // Retrieve pairs/sequences of usable commits
        Set<CommitPair<SPLCommit>> pairs = dataset.getCommitPairsForEvolutionStudy();
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

        // TODO: Handle empty diff between source versions
        // For each pair
        for (CommitPair<SPLCommit> pair : pairs) {
            // Take next commit pair
            SPLCommit commitV0 = pair.parent();
            SPLCommit commitV1 = pair.child();

            // Checkout the commits in the SPL repository
            try {
                splRepositoryV0.checkoutCommit(commitV0);
                splRepositoryV1.checkoutCommit(commitV1);
            } catch (GitAPIException | IOException e) {
                panic("Was not able to checkout commit for SPL repository.", e);
            }

            IFeatureModel modelV0 = commitV0.featureModel().run().orElseThrow();
            IFeatureModel modelV1 = commitV1.featureModel().run().orElseThrow();
            final Node featureModelFormulaV0 = new FeatureModelFormula(modelV0).getPropositionalNode();
            final Node featureModelFormulaV1 = new FeatureModelFormula(modelV0).getPropositionalNode();

            // While more random configurations to consider
            for (int i = 0; i < randomRepeats; i++) {
                // Sample set of random variants
                Sample configurations = variantSampler.sample(modelV0);

                // Generate the randomly selected variants at both versions
                for (Variant variant : configurations.variants()) {
                    // TODO: Check whether this is enough. It only checks satisfiability, but might not check whether the variables exist in the feature model. Maybe check whether the set of selected variables exists?
                    if (!variant.isImplementing(featureModelFormulaV0)) {
                        panic("Sampled " + variant + " is not valid for feature model " + modelV0 + "!");
                    }
                    if (!variant.isImplementing(featureModelFormulaV1)) {
                        panic("Sampled " + variant + " is not valid for feature model " + modelV1 + "!");
                    }
                    commitV0.presenceConditions().run().orElseThrow().generateVariant(variant, new CaseSensitivePath(splRepositoryV0.getPath()), variantsDirV0.resolve(variant.getName()), VariantGenerationOptions.ExitOnError);
                    commitV1.presenceConditions().run().orElseThrow().generateVariant(variant, new CaseSensitivePath(splRepositoryV1.getPath()), variantsDirV1.resolve(variant.getName()), VariantGenerationOptions.ExitOnError);
                }

                // Select next source variant
                for (Variant source : configurations.variants()) {
                    // apply diff to both versions of source variant
                    OriginalDiff originalDiff = getOriginalDiff(variantsDirV0.path().resolve(source.getName()), variantsDirV1.path().resolve(source.getName()));
                    FineDiff fineDiff = getFineDiff(originalDiff);

                    // Save the fine Diff
                    try {
                        Files.write(normalPatchFile, fineDiff.toLines());
                    } catch (IOException e) {
                        panic("Was not able to save FineDiff to file " + normalPatchFile);
                    }

                    // for each target variant
                    for (Variant target : configurations.variants()) {
                        if (target == source) {
                            continue;
                        }
                        // TODO: Extract creation of diff, saving of diff, creation of copy, application of patch, evaluation of result, and clean-up
                        // copy target variant
                        shell.execute(new CpCommand(variantsDirV0.path().resolve(target.getName()), patchDir).recursive()).expect("Was not able to copy variant " + target.getName());

                        /* Application of patches without knowledge about features */
                        // apply patch to copied target variant
                        shell.execute(PatchCommand.Recommended(normalPatchFile), patchDir);
                        // diff patch result and target variant
                        {
                            DiffCommand diffCommand = DiffCommand.Recommended(variantsDirV0.resolve(source.getName()).path(), variantsDirV1.resolve(source.getName()).path());
                            List<String> output = shell.execute(diffCommand).expect("Was not able to diff variants.");
                            // evaluate diff
                            if (output.isEmpty()) {
                                // TODO
                                Logger.info("No difference between patched variant and actual target");
                            } else {
                                // TODO
                                Logger.warning("There are differences between the patched variant and the actual target");
                            }
                        }
                        // evaluate patch rejects
                        evaluatePatchRejects();
                        
                        /* Application of patches with knowledge about features */
                        // Clean patch directory
                        // TODO: 
                        // Create target variant specific patch that respects PCs
                        FineDiff pcBasedDiff = getPCBasedDiff(originalDiff, commitV0, commitV1, target);
                        // Save the fine Diff
                        try {
                            Files.write(pcBasedPatchFile, pcBasedDiff.toLines());
                        } catch (IOException e) {
                            panic("Was not able to save pc-based diff to file " + pcBasedPatchFile);
                        }
                        // copy target variant
                        shell.execute(new CpCommand(variantsDirV0.path().resolve(target.getName()), patchDir).recursive()).expect("Was not able to copy variant " + target.getName());
                        // apply patch to copied target variant
                        shell.execute(PatchCommand.Recommended(pcBasedPatchFile), patchDir);
                        // diff patch result and target variant
                        // evaluate diff
                        // evaluate patch rejects
                    }
                }
            }
        }
    }
    
    private static void evaluatePatchRejects() {
        // TODO: Implement evaluation
        Logger.error("TODO");
    }

    @NotNull
    private static FineDiff getFineDiff(OriginalDiff originalDiff) {
        DefaultContextProvider contextProvider = new DefaultContextProvider(workDir);
        return DiffSplitter.split(originalDiff, contextProvider);
    }
    
    private static FineDiff getPCBasedDiff(OriginalDiff originalDiff, SPLCommit commitV0, SPLCommit commitV1, Variant target) {
        Artefact tracesV0 = commitV0.presenceConditions().run().orElseThrow();
        Artefact tracesV1 = commitV1.presenceConditions().run().orElseThrow();
        PCBasedFilter pcBasedFilter = new PCBasedFilter(tracesV0, tracesV1, target, variantsDirV0.path(), variantsDirV1.path());
        // Create target variant specific patch that respects PCs
        DefaultContextProvider contextProvider = new DefaultContextProvider(workDir);
        return DiffSplitter.split(originalDiff, pcBasedFilter, pcBasedFilter, contextProvider);
    }
    
    private static OriginalDiff getOriginalDiff(Path v0Path, Path v1Path) {
        DiffCommand diffCommand = DiffCommand.Recommended(v0Path, v1Path);
        List<String> output = shell.execute(diffCommand).expect("Was not able to diff variants.");
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