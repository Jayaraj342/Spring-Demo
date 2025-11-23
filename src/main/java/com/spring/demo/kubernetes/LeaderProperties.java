package com.spring.demo.kubernetes;

import java.time.Duration;

public class LeaderProperties {
    private static final boolean DEFAULT_ENABLED = true;
    private static final String DEFAULT_LEADER_ID_PREFIX = "leader.id.";
    private static final boolean DEFAULT_AUTO_STARTUP = true;
    private static final String DEFAULT_CONFIG_MAP_NAME = "leaders";
    private static final Duration DEFAULT_UPDATE_PERIOD = Duration.ofMillis(60000L);
    private static final boolean DEFAULT_PUBLISH_FAILED_EVENTS = false;
    private static final boolean DEFAULT_CREATE_CONFIG_MAP = true;
    private boolean enabled = true;
    private boolean autoStartup = true;
    private String role;
    private String namespace;
    private String configMapName = "leaders";
    private String leaderIdPrefix = "leader.id.";
    private Duration updatePeriod;
    private boolean publishFailedEvents;
    private boolean createConfigMap;

    public LeaderProperties() {
        this.updatePeriod = DEFAULT_UPDATE_PERIOD;
        this.publishFailedEvents = false;
        this.createConfigMap = true;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAutoStartup() {
        return this.autoStartup;
    }

    public void setAutoStartup(boolean autoStartup) {
        this.autoStartup = autoStartup;
    }

    public String getRole() {
        return this.role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getNamespace(String defaultValue) {
        return this.namespace != null && !this.namespace.isEmpty() ? this.namespace : defaultValue;
    }

    public String getConfigMapName() {
        return this.configMapName;
    }

    public void setConfigMapName(String configMapName) {
        this.configMapName = configMapName;
    }

    public String getLeaderIdPrefix() {
        return this.leaderIdPrefix;
    }

    public void setLeaderIdPrefix(String leaderIdPrefix) {
        this.leaderIdPrefix = leaderIdPrefix;
    }

    public Duration getUpdatePeriod() {
        return this.updatePeriod;
    }

    public void setUpdatePeriod(Duration updatePeriod) {
        this.updatePeriod = updatePeriod;
    }

    public boolean isPublishFailedEvents() {
        return this.publishFailedEvents;
    }

    public void setPublishFailedEvents(boolean publishFailedEvents) {
        this.publishFailedEvents = publishFailedEvents;
    }

    public boolean isCreateConfigMap() {
        return this.createConfigMap;
    }

    public void setCreateConfigMap(boolean createConfigMap) {
        this.createConfigMap = createConfigMap;
    }
}
