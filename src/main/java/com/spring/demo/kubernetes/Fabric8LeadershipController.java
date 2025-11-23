package com.spring.demo.kubernetes;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapFluent;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.leader.Candidate;
import org.springframework.integration.leader.event.LeaderEventPublisher;

import java.util.Map;

public class Fabric8LeadershipController extends LeadershipController {
    private static final Logger LOGGER = LoggerFactory.getLogger(Fabric8LeadershipController.class);
    private final KubernetesClient kubernetesClient;

    public Fabric8LeadershipController(Candidate candidate, LeaderProperties leaderProperties, LeaderEventPublisher leaderEventPublisher, KubernetesClient kubernetesClient) {
        super(candidate, leaderProperties, leaderEventPublisher);
        this.kubernetesClient = kubernetesClient;
    }

    public synchronized void update() {
        LOGGER.debug("Checking leader state");
        ConfigMap configMap = this.getConfigMap();
        if (configMap == null && !this.leaderProperties.isCreateConfigMap()) {
            LOGGER.warn("ConfigMap '{}' does not exist and leaderProperties.isCreateConfigMap() is false, cannot acquire leadership", this.leaderProperties.getConfigMapName());
            this.notifyOnFailedToAcquire();
        } else {
            Leader leader = this.extractLeader(configMap);
            if (leader != null && this.isPodReady(leader.getId())) {
                this.handleLeaderChange(leader);
            } else {
                if (leader != null && leader.isCandidate(this.candidate)) {
                    this.revoke(configMap);
                } else {
                    this.acquire(configMap);
                }
            }
        }
    }

    public synchronized void revoke() {
        ConfigMap configMap = this.getConfigMap();
        Leader leader = this.extractLeader(configMap);
        if (leader != null && leader.isCandidate(this.candidate)) {
            this.revoke(configMap);
        }
    }

    private void revoke(ConfigMap configMap) {
        LOGGER.debug("Trying to revoke leadership for '{}'", this.candidate);

        try {
            String leaderKey = this.getLeaderKey();
            this.removeConfigMapEntry(configMap, leaderKey);
            this.handleLeaderChange((Leader) null);
        } catch (KubernetesClientException var3) {
            LOGGER.warn("Failure when revoking leadership for '{}': {}", this.candidate, var3.getMessage());
        }
    }

    private void acquire(ConfigMap configMap) {
        LOGGER.debug("Trying to acquire leadership for '{}'", this.candidate);
        if (!this.isPodReady(this.candidate.getId())) {
            LOGGER.debug("Pod of '{}' is not ready at the moment, cannot acquire leadership", this.candidate);
        } else {
            try {
                Map<String, String> data = this.getLeaderData(this.candidate);
                if (configMap == null) {
                    this.createConfigMap(data);
                } else {
                    this.updateConfigMapEntry(configMap, data);
                }

                Leader newLeader = new Leader(this.candidate.getRole(), this.candidate.getId());
                this.handleLeaderChange(newLeader);
            } catch (KubernetesClientException var4) {
                LOGGER.warn("Failure when acquiring leadership for '{}': {}", this.candidate, var4.getMessage());
                this.notifyOnFailedToAcquire();
            }
        }
    }

    protected PodReadinessWatcher createPodReadinessWatcher(String localLeaderId) {
        return new Fabric8PodReadinessWatcher(localLeaderId, this.kubernetesClient, this);
    }

    private Leader extractLeader(ConfigMap configMap) {
        return configMap == null ? null : this.extractLeader(configMap.getData());
    }

    private boolean isPodReady(String name) {
        return ((PodResource) this.kubernetesClient.pods().withName(name)).isReady();
    }

    private ConfigMap getConfigMap() {
        return (ConfigMap) ((Resource) ((NonNamespaceOperation) this.kubernetesClient.configMaps().inNamespace(this.leaderProperties.getNamespace(this.kubernetesClient.getNamespace()))).withName(this.leaderProperties.getConfigMapName())).get();
    }

    private void createConfigMap(Map<String, String> data) {
        LOGGER.debug("Creating new config map with data: {}", data);
        ConfigMap newConfigMap = ((ConfigMapBuilder) ((ConfigMapBuilder) ((ConfigMapFluent.MetadataNested) ((ConfigMapFluent.MetadataNested) ((ConfigMapFluent.MetadataNested) (new ConfigMapBuilder()).withNewMetadata().withName(this.leaderProperties.getConfigMapName())).addToLabels("provider", "spring-cloud-kubernetes")).addToLabels("kind", "leaders")).endMetadata()).addToData(data)).build();
        ((Resource) ((NonNamespaceOperation) this.kubernetesClient.configMaps().inNamespace(this.leaderProperties.getNamespace(this.kubernetesClient.getNamespace()))).resource(newConfigMap)).create();
    }

    private void updateConfigMapEntry(ConfigMap configMap, Map<String, String> newData) {
        LOGGER.debug("Adding new data to config map: {}", newData);
        ConfigMap newConfigMap = ((ConfigMapBuilder) (new ConfigMapBuilder(configMap)).addToData(newData)).build();
        this.updateConfigMap(configMap, newConfigMap);
    }

    private void removeConfigMapEntry(ConfigMap configMap, String key) {
        LOGGER.debug("Removing config map entry '{}'", key);
        ConfigMap newConfigMap = ((ConfigMapBuilder) (new ConfigMapBuilder(configMap)).removeFromData(key)).build();
        this.updateConfigMap(configMap, newConfigMap);
    }

    private void updateConfigMap(ConfigMap oldConfigMap, ConfigMap newConfigMap) {
        ((Resource) ((NonNamespaceOperation) this.kubernetesClient.configMaps().inNamespace(this.leaderProperties.getNamespace(this.kubernetesClient.getNamespace()))).resource(newConfigMap)).lockResourceVersion(oldConfigMap.getMetadata().getResourceVersion()).replace();
    }
}

