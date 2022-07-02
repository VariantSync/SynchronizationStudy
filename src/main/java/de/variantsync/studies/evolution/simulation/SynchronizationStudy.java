package de.variantsync.studies.evolution.simulation;

import de.variantsync.studies.evolution.simulation.experiment.ExperimentBusyBox;
import de.variantsync.studies.evolution.simulation.experiment.ExperimentConfiguration;
import de.variantsync.studies.evolution.simulation.experiment.EExperimentalSubject;
import de.variantsync.studies.evolution.simulation.experiment.Experiment;

import java.io.File;

public class SynchronizationStudy {
    public static void main(final String... args) {
        if (args.length < 1) {
            System.err.println("The first argument should provide the path to the configuration file that is to be used");
        }
        final ExperimentConfiguration config = new ExperimentConfiguration(new File(args[0]));

        final Experiment experiment;
        if (config.EXPERIMENT_SUBJECT() == EExperimentalSubject.BUSYBOX) {
            experiment = new ExperimentBusyBox(config);
        } else {
            throw new IllegalArgumentException("Only BUSYBOX is possible.");
        }

        try {
            experiment.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            System.exit(0);
        }
    }
}