import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.variantsync.evolution.Main;
import de.variantsync.evolution.feature.Variant;
import de.variantsync.evolution.feature.config.FeatureIDEConfiguration;
import de.variantsync.evolution.io.ResourceLoader;
import de.variantsync.evolution.io.kernelhaven.KernelHavenVariantPCIO;
import de.variantsync.evolution.util.NotImplementedException;
import de.variantsync.evolution.util.fide.FeatureModelUtils;
import de.variantsync.evolution.variability.pc.Artefact;
import de.variantsync.studies.sync.diff.DiffParser;
import de.variantsync.studies.sync.diff.components.FineDiff;
import de.variantsync.studies.sync.diff.filter.IFileDiffFilter;
import de.variantsync.studies.sync.diff.filter.ILineFilter;
import de.variantsync.studies.sync.diff.filter.PCBasedFilter;
import de.variantsync.studies.sync.diff.splitting.DefaultContextProvider;
import de.variantsync.studies.sync.diff.splitting.DiffSplitter;
import de.variantsync.studies.sync.diff.splitting.IContextProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class PCBasedFilterTest {

    Path resourceDir = Paths.get("src", "test", "resources", "pc-filter");
    Path splitsDir = resourceDir.resolve("splits");

    static {
        Main.Initialize();
    }

    private void runComparison(Path pathToExpectedResult, IFileDiffFilter fileFilter, ILineFilter lineFilter) throws IOException {
        List<String> diffLines = Files.readAllLines(resourceDir.resolve("diff-A-B.txt"));
        IContextProvider contextProvider = new DefaultContextProvider(resourceDir);
        FineDiff fineDiff = DiffSplitter.split(DiffParser.toOriginalDiff(diffLines), fileFilter, lineFilter, contextProvider);

        List<String> expectedLines = Files.readAllLines(pathToExpectedResult);
        List<String> actualLines = fineDiff.toLines();
        Assertions.assertEquals(expectedLines, actualLines);
    }
    
    private PCBasedFilter getPCBasedFilter(Artefact oldTraces, Artefact newTraces, Variant variant, Path oldVersion, Path newVersion) {
        return new PCBasedFilter(oldTraces, newTraces, variant, oldVersion, newVersion);
    }

    private PCBasedFilter getPCBasedFilter(Variant variant) {
        ResourceLoader<Artefact> artefactLoader = new KernelHavenVariantPCIO();
        Artefact oldTraces = artefactLoader.load(resourceDir.resolve("filters/version-A.variant.csv")).expect("");
        Artefact newTraces = artefactLoader.load(resourceDir.resolve("filters/version-B.variant.csv")).expect("");
        Path oldVersion = Path.of("version-A");
        Path newVersion = Path.of("version-B");
        return getPCBasedFilter(oldTraces, newTraces, variant, oldVersion, newVersion);
    }

    @Test
    public void basicValidation() throws IOException {
        runComparison(resourceDir.resolve("fine-diff-A-B.txt"), null, null);
    }
    
    @Test
    public void removedLineNotInTargetVariant() throws IOException {
        IFeatureModel model = FeatureModelUtils.FromOptionalFeatures("A", "B", "C", "D", "E", "LEAD", "TRAIL");
        final FeatureModelFormula fmf = new FeatureModelFormula(model);
        Variant variant = new Variant("Target", new FeatureIDEConfiguration(fmf, Arrays.asList("B", "C", "D", "E", "LEAD", "TRAIL")));
        PCBasedFilter pcBasedFilter = getPCBasedFilter(variant);
        runComparison(splitsDir.resolve("removedLineNotInTargetVariant.txt"), pcBasedFilter, pcBasedFilter);
    }

    @Test
    public void addedLineNotInTargetVariant() throws IOException {
        IFeatureModel model = FeatureModelUtils.FromOptionalFeatures("A", "B", "C", "D", "E", "LEAD", "TRAIL");
        final FeatureModelFormula fmf = new FeatureModelFormula(model);
        Variant variant = new Variant("Target", new FeatureIDEConfiguration(fmf, Arrays.asList("A", "B", "C", "D", "LEAD", "TRAIL")));
        PCBasedFilter pcBasedFilter = getPCBasedFilter(variant);
        runComparison(splitsDir.resolve("addedLineNotInTargetVariant.txt"), pcBasedFilter, pcBasedFilter);
    }

    @Test
    public void leadContextLineNotInTargetVariant() throws IOException {
        IFeatureModel model = FeatureModelUtils.FromOptionalFeatures("A", "B", "C", "D", "E", "LEAD", "TRAIL");
        final FeatureModelFormula fmf = new FeatureModelFormula(model);
        Variant variant = new Variant("Target", new FeatureIDEConfiguration(fmf, Arrays.asList("A", "B", "C", "D", "E", "TRAIL")));
        PCBasedFilter pcBasedFilter = getPCBasedFilter(variant);
        runComparison(splitsDir.resolve("leadContextLineNotInTargetVariant.txt"), pcBasedFilter, pcBasedFilter);
    }

    @Test
    public void trailContextLineNotInTargetVariant() throws IOException {
        IFeatureModel model = FeatureModelUtils.FromOptionalFeatures("A", "B", "C", "D", "E", "LEAD", "TRAIL");
        final FeatureModelFormula fmf = new FeatureModelFormula(model);
        Variant variant = new Variant("Target", new FeatureIDEConfiguration(fmf, Arrays.asList("A", "B", "C", "D", "E", "LEAD")));
        PCBasedFilter pcBasedFilter = getPCBasedFilter(variant);
        runComparison(splitsDir.resolve("trailContextLineNotInTargetVariant.txt"), pcBasedFilter, pcBasedFilter);
    }

    @Test
    public void entireFileNotInTargetVariant() {
        throw new NotImplementedException();
    }

    @Test
    public void unrelatedLinesNotInTargetVariant() {
        throw new NotImplementedException();
    }
}
