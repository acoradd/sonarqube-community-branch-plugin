package com.github.mc1arke.sonarqube.plugin.worker.provider;

import org.sonar.api.config.Configuration;
import org.sonar.ce.configuration.WorkerCountProvider;

import static com.github.mc1arke.sonarqube.plugin.worker.property.CommunityWorkerProperty.WORKER_PROPERTIES;

public class CommunityWorkerCountProvider implements WorkerCountProvider {

    private final Configuration configuration;

    public CommunityWorkerCountProvider(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public int get() {
        return Math.min(10, configuration.getInt(WORKER_PROPERTIES).orElse(2));
    }
}
