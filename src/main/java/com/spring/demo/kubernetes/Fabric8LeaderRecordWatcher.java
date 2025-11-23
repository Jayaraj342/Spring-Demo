package com.spring.demo.kubernetes;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fabric8LeaderRecordWatcher implements LeaderRecordWatcher, Watcher<ConfigMap> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Fabric8LeaderRecordWatcher.class);
    private final Object lock = new Object();
    private final Fabric8LeadershipController fabric8LeadershipController;
    private final LeaderProperties leaderProperties;
    private final KubernetesClient kubernetesClient;
    private Watch watch;

    public Fabric8LeaderRecordWatcher(LeaderProperties leaderProperties, Fabric8LeadershipController fabric8LeadershipController, KubernetesClient kubernetesClient) {
        this.fabric8LeadershipController = fabric8LeadershipController;
        this.leaderProperties = leaderProperties;
        this.kubernetesClient = kubernetesClient;
    }

    public void start() {
        if (this.watch == null) {
            synchronized(this.lock) {
                if (this.watch == null) {
                    LOGGER.debug("Starting leader record watcher");
                    this.watch = ((Resource)((NonNamespaceOperation)this.kubernetesClient.configMaps().inNamespace(this.leaderProperties.getNamespace(this.kubernetesClient.getNamespace()))).withName(this.leaderProperties.getConfigMapName())).watch(this);
                }
            }
        }

    }

    public void stop() {
        if (this.watch != null) {
            synchronized(this.lock) {
                if (this.watch != null) {
                    LOGGER.debug("Stopping leader record watcher");
                    this.watch.close();
                    this.watch = null;
                }
            }
        }

    }

    public void eventReceived(Watcher.Action action, ConfigMap configMap) {
        LOGGER.debug("'{}' event received, triggering leadership update", action);
        if (!Action.ERROR.equals(action)) {
            this.fabric8LeadershipController.update();
        }

    }

    public void onClose(WatcherException cause) {
        if (cause != null) {
            synchronized(this.lock) {
                LOGGER.warn("Watcher stopped unexpectedly, will restart", cause);
                this.watch = null;
                this.start();
            }
        }

    }
}

