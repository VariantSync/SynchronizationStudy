import de.ovgu.featureide.fm.core.base.impl.ConfigurationFactoryManager;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.variantsync.evolution.io.kernelhaven.KernelHavenPCIO;
import de.variantsync.evolution.io.kernelhaven.KernelHavenVariantPCIO;
import de.variantsync.evolution.util.NotImplementedException;
import de.variantsync.evolution.util.fide.FeatureModelUtils;
import de.variantsync.evolution.variability.config.FeatureIDEConfiguration;
import de.variantsync.evolution.variability.config.IConfiguration;
import de.variantsync.studies.sync.diff.DiffParser;
import de.variantsync.studies.sync.diff.components.FineDiff;
import de.variantsync.studies.sync.diff.filter.ILineFilter;
import de.variantsync.studies.sync.diff.splitting.DefaultContextProvider;
import de.variantsync.studies.sync.diff.splitting.DiffSplitter;
import de.variantsync.studies.sync.diff.splitting.IContextProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class PCBasedFilterTest {

    Path resourceDir = Paths.get("src", "test", "resources", "pc-filter", "splits");

    private void runComparison(Path pathToExpectedResult, ILineFilter lineFilter) throws IOException {
        List<String> diffLines = Files.readAllLines(resourceDir.getParent().resolve("diff-A-B.txt"));
        IContextProvider contextProvider = new DefaultContextProvider(resourceDir.getParent());
        FineDiff fineDiff = DiffSplitter.split(DiffParser.toOriginalDiff(diffLines), null, lineFilter, contextProvider);

        List<String> expectedLines = Files.readAllLines(pathToExpectedResult);
        List<String> actualLines = fineDiff.toLines();
        Assertions.assertEquals(expectedLines, actualLines);
    }
    
    @Test
    public void test() {
        
//        FeatureIDEConfiguration configuration = new FeatureIDEConfiguration();
        KernelHavenPCIO pcIO = new KernelHavenVariantPCIO();
        var result = pcIO.load(Path.of("path.variant.csv"));
    }

    @Test
    public void basicValidation() throws IOException {
        runComparison(resourceDir.getParent().resolve("fine-diff-A-B.txt"), null);
    }
    
    @Test
    public void removedLineNotInTargetVariant() {
        throw new NotImplementedException();
    }

    @Test
    public void addedLineNotInTargetVariant() {
        throw new NotImplementedException();
    }

    @Test
    public void leadContextLineNotInTargetVariant() {
        throw new NotImplementedException();
    }

    @Test
    public void trailContextLineNotInTargetVariant() {
        throw new NotImplementedException();
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
