package com.spring.demo.kubernetes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LeaderInitiator implements SmartLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderInitiator.class);
    private final LeaderProperties leaderProperties;
    private final LeadershipController leadershipController;
    private final LeaderRecordWatcher leaderRecordWatcher;
    private final PodReadinessWatcher hostPodWatcher;
    private ScheduledExecutorService scheduledExecutorService;
    private boolean isRunning;

    public LeaderInitiator(LeaderProperties leaderProperties, LeadershipController leadershipController, LeaderRecordWatcher leaderRecordWatcher, PodReadinessWatcher hostPodWatcher) {
        this.leaderProperties = leaderProperties;
        this.leadershipController = leadershipController;
        this.leaderRecordWatcher = leaderRecordWatcher;
        this.hostPodWatcher = hostPodWatcher;
    }

    public boolean isAutoStartup() {
        return this.leaderProperties.isAutoStartup();
    }

    public void start() {
        if (!this.isRunning()) {
            LOGGER.debug("Leader initiator starting");
            this.leaderRecordWatcher.start();
            this.hostPodWatcher.start();
            this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            ScheduledExecutorService var10000 = this.scheduledExecutorService;
            LeadershipController var10001 = this.leadershipController;
            Objects.requireNonNull(var10001);
            var10000.scheduleAtFixedRate(var10001::update, this.leaderProperties.getUpdatePeriod().toMillis(), this.leaderProperties.getUpdatePeriod().toMillis(), TimeUnit.MILLISECONDS);
            this.isRunning = true;
        }

    }

    public void stop() {
        if (this.isRunning()) {
            LOGGER.debug("Leader initiator stopping");
            this.scheduledExecutorService.shutdown();
            this.scheduledExecutorService = null;
            this.hostPodWatcher.stop();
            this.leaderRecordWatcher.stop();
            this.leadershipController.revoke();
            this.isRunning = false;
        }

    }

    public void stop(Runnable callback) {
        this.stop();
        callback.run();
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    public int getPhase() {
        return 0;
    }
}

