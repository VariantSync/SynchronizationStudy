package de.variantsync.studies.sync;

import de.variantsync.studies.sync.experiment.*;

import java.io.File;

public class SynchronizationStudy {
    public static void main(final String... args) {
        if (args.length < 1) {
            System.err.println("The first argument should provide the path to the configuration file that is to be used");
        }
        final ExperimentConfiguration config = new ExperimentConfiguration(new File(args[0]));

        final Experiment experiment;
        switch (config.EXPERIMENT_SUBJECT()) {
            case BUSYBOX -> experiment = new ExperimentBusyBox(config);
            case LINUX -> experiment = new ExperimentLinux(config);
            default -> throw new IllegalArgumentException("Only BUSYBOX and LINUX are possible.");
        }

        experiment.run();
    }
}