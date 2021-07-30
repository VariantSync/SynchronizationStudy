package de.variantsync.studies.sync;

import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.variantsync.evolution.feature.Sample;
import de.variantsync.evolution.feature.Variant;
import de.variantsync.evolution.io.Resources;
import de.variantsync.evolution.util.CaseSensitivePath;
import de.variantsync.evolution.util.NotImplementedException;
import de.variantsync.evolution.util.list.NonEmptyList;
import de.variantsync.evolution.variability.CommitPair;
import de.variantsync.evolution.variability.SPLCommit;
import de.variantsync.evolution.variability.VariabilityDataset;
import de.variantsync.evolution.variability.pc.VariantGenerationOptions;
import de.variantsync.studies.sync.error.SetupError;

import java.nio.file.Path;
import java.util.*;

public class SynchronizationStudy {
    // TODO: Set in external config
    private static final Path datasetPath = Path.of("");
    
    public static void main(String... args) {
        // Load VariabilityDataset
        VariabilityDataset dataset;
        try {
            dataset = Resources.Instance().load(VariabilityDataset.class, datasetPath);
        } catch (Resources.ResourceIOException e) {
            throw new SetupError("Was not able to load dataset.");
        }
        
        // Retrieve pairs/sequences of usable commits
        Set<CommitPair<SPLCommit>> pairs = dataset.getCommitPairsForEvolutionStudy();
        var history = dataset.getVariabilityHistory(setOfCommits -> {
            List<NonEmptyList<SPLCommit>> sequences = new ArrayList<>(pairs.size());
            pairs.stream().map(p -> {
                List<SPLCommit> list = new ArrayList<>(2);
                list.add(p.parent());
                list.add(p.child());
                return new NonEmptyList<>(list);
            }).forEach(sequences::add);
            return sequences;
        });
        
        // For each pair
        for (CommitPair<SPLCommit> pair : pairs) {
            // Take next commit pair
            SPLCommit oldCommit = pair.parent();
            SPLCommit newCommit = pair.child();
            // While more random configurations to consider
            // Sample set of random configuration
//            FeatureIDESampler variantSampler = FeatureIDESampler.CreateRandomSample(n);
//            Sample configurations = variantSampler.sample(oldCommit.featureModel());
            // Generate variants
            CaseSensitivePath splDir = null;
            CaseSensitivePath variantsDir = null;
            oldCommit.presenceConditions().run().get().generateVariant(null, splDir, variantsDir, VariantGenerationOptions.ExitOnError);
            newCommit.presenceConditions().run().get().generateVariant(null, splDir, variantsDir, VariantGenerationOptions.ExitOnError);
            /*
            final Node featureModelFormula = new FeatureModelFormula(fm).getPropositionalNode();
            // TODO: Check whether this is enough. It only checks satisfiability, but might not check whether the variables exist in the feature model. Maybe check whether the set of selected variables exists?
            if (!variant.isImplementing(featureModelFormula)) {
               throw new IllegalSampleException("Sampled " + variant + " is not valid anymore for feature model " + model + "!");
            }
             */
            // Select next source variant
            // copy source variant
            // checkout previous revision of copied source variant
            
            // apply diff to source variant and its copy to receive patch
            
            // for each target variant
            // select next target
            // copy target variant
            // checkout previous revision of copied target variant
            // apply patch to copied target variant
            // diff patch result and target variant
            // evaluate diff
            // evaluate patch rejects
        }
        
    }
    
}