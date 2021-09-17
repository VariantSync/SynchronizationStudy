package de.variantsync.studies.sync.experiment;

import de.variantsync.evolution.util.LogLevel;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;

public class ExperimentConfiguration {
    private final Configuration config;
    public final String EXPERIMENT_NAME = "experiment.name";
    public final String EXPERIMENT_REPEATS = "experiment.repeats";
    public final String EXPERIMENT_VARIANT_COUNT = "experiment.variant.count";
    public final String EXPERIMENT_DIR_MAIN = "experiment.dir.main";
    public final String EXPERIMENT_DIR_DATASET = "experiment.dir.dataset";
    public final String EXPERIMENT_DIR_SPL = "experiment.dir.spl";
    public final String EXPERIMENT_LOGGER_LEVEL = "experiment.logger.level";

    public ExperimentConfiguration(File propertiesFile) {
        Parameters params = new Parameters();
        try {
            var builder =
                    new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                            .configure(params.properties()
                                    .setFile(propertiesFile)
                                    .setListDelimiterHandler(new DefaultListDelimiterHandler(',')));
            this.config = builder.getConfiguration();
        } catch (ConfigurationException e) {
            System.err.println("Was not able to load properties file " + propertiesFile);
            throw new RuntimeException(e);
        }
    }

    public String EXPERIMENT_NAME() {
        return config.getString(EXPERIMENT_NAME);
    }

    public int EXPERIMENT_REPEATS() {
        return config.getInt(EXPERIMENT_REPEATS);
    }

    public int EXPERIMENT_VARIANT_COUNT() {
        return config.getInt(EXPERIMENT_VARIANT_COUNT);
    }

    public String EXPERIMENT_DIR_MAIN() {
        return config.getString(EXPERIMENT_DIR_MAIN);
    }

    public String EXPERIMENT_DIR_DATASET() {
        return config.getString(EXPERIMENT_DIR_DATASET);
    }

    public String EXPERIMENT_DIR_SPL() {
        return config.getString(EXPERIMENT_DIR_SPL);
    }

    public LogLevel EXPERIMENT_LOGGER_LEVEL() {
        return LogLevel.valueOf(config.getString(EXPERIMENT_LOGGER_LEVEL));
    }
}
