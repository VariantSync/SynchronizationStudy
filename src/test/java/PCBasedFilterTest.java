import de.ovgu.featureide.fm.core.base.impl.ConfigurationFactoryManager;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.variantsync.evolution.io.kernelhaven.KernelHavenPCIO;
import de.variantsync.evolution.io.kernelhaven.KernelHavenVariantPCIO;
import de.variantsync.evolution.util.fide.FeatureModelUtils;
import de.variantsync.evolution.variability.config.FeatureIDEConfiguration;
import de.variantsync.evolution.variability.config.IConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public class PCBasedFilterTest {
    
    @Test
    public void test() {
        
//        FeatureIDEConfiguration configuration = new FeatureIDEConfiguration();
        KernelHavenPCIO pcIO = new KernelHavenVariantPCIO();
        var result = pcIO.load(Path.of("path.variant.csv"));
    }
}
