package de.variantsync.studies.sync.experiment;

import de.variantsync.evolution.feature.sampling.ConstSampler;
import de.variantsync.evolution.feature.sampling.LinuxKernel;
import de.variantsync.evolution.feature.sampling.Sample;
import de.variantsync.evolution.feature.sampling.Sampler;
import de.variantsync.evolution.variability.SPLCommit;

import java.io.IOException;
import java.io.UncheckedIOException;

public class ExperimentLinux extends Experiment{
    private final Sampler sampler;

    public ExperimentLinux(final ExperimentConfiguration config) {
        super(config);
        try {
            sampler = new ConstSampler(LinuxKernel.GetSample());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected Sample sample(final SPLCommit commitV0, final SPLCommit commitV1) {
        return sampler.sample(null);
    }
}
