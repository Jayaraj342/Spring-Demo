package com.spring.demo.kubernetes;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.readiness.Readiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fabric8PodReadinessWatcher implements PodReadinessWatcher, Watcher<Pod> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Fabric8PodReadinessWatcher.class);
    private final Object lock = new Object();
    private final String podName;
    private final KubernetesClient kubernetesClient;
    private final Fabric8LeadershipController fabric8LeadershipController;
    private boolean previousState;
    private Watch watch;

    public Fabric8PodReadinessWatcher(String podName, KubernetesClient kubernetesClient, Fabric8LeadershipController fabric8LeadershipController) {
        this.podName = podName;
        this.kubernetesClient = kubernetesClient;
        this.fabric8LeadershipController = fabric8LeadershipController;
    }

    public void start() {
        if (this.watch == null) {
            synchronized(this.lock) {
                if (this.watch == null) {
                    LOGGER.debug("Starting pod readiness watcher for '{}'", this.podName);
                    PodResource podResource = (PodResource)this.kubernetesClient.pods().withName(this.podName);
                    this.previousState = podResource.isReady();
                    this.watch = podResource.watch(this);
                }
            }
        }

    }

    public void stop() {
        if (this.watch != null) {
            synchronized(this.lock) {
                if (this.watch != null) {
                    LOGGER.debug("Stopping pod readiness watcher for '{}'", this.podName);
                    this.watch.close();
                    this.watch = null;
                }
            }
        }

    }

    public void eventReceived(Watcher.Action action, Pod pod) {
        boolean currentState = Readiness.isPodReady(pod);
        if (this.previousState != currentState) {
            synchronized(this.lock) {
                if (this.previousState != currentState) {
                    LOGGER.debug("'{}' readiness status changed to '{}', triggering leadership update", this.podName, currentState);
                    this.previousState = currentState;
                    this.fabric8LeadershipController.update();
                }
            }
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

