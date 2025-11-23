package com.spring.demo.kubernetes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.leader.Candidate;
import org.springframework.integration.leader.Context;
import org.springframework.integration.leader.event.LeaderEventPublisher;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class LeadershipController {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeadershipController.class);
    protected static final String PROVIDER_KEY = "provider";
    protected static final String PROVIDER = "spring-cloud-kubernetes";
    protected static final String KIND_KEY = "kind";
    protected static final String KIND = "leaders";
    protected Candidate candidate;
    protected Leader localLeader;
    protected LeaderProperties leaderProperties;
    protected LeaderEventPublisher leaderEventPublisher;
    protected PodReadinessWatcher leaderReadinessWatcher;

    public LeadershipController(Candidate candidate, LeaderProperties leaderProperties, LeaderEventPublisher leaderEventPublisher) {
        this.candidate = candidate;
        this.leaderProperties = leaderProperties;
        this.leaderEventPublisher = leaderEventPublisher;
    }

    public Optional<Leader> getLocalLeader() {
        return Optional.ofNullable(this.localLeader);
    }

    public abstract void update();

    public abstract void revoke();

    protected String getLeaderKey() {
        String var10000 = this.leaderProperties.getLeaderIdPrefix();
        return var10000 + this.candidate.getRole();
    }

    protected Map<String, String> getLeaderData(Candidate candidate) {
        String leaderKey = this.getLeaderKey();
        return Collections.singletonMap(leaderKey, candidate.getId());
    }

    protected Leader extractLeader(Map<String, String> data) {
        if (data == null) {
            return null;
        } else {
            String leaderKey = this.getLeaderKey();
            String leaderId = (String)data.get(leaderKey);
            return !StringUtils.hasText(leaderId) ? null : new Leader(this.candidate.getRole(), leaderId);
        }
    }

    protected void handleLeaderChange(Leader newLeader) {
        if (Objects.equals(this.localLeader, newLeader)) {
            LOGGER.debug("Leader is still '{}'", this.localLeader);
        } else {
            Leader oldLeader = this.localLeader;
            this.localLeader = newLeader;
            if (oldLeader != null && oldLeader.isCandidate(this.candidate)) {
                this.notifyOnRevoked();
            } else if (newLeader != null && newLeader.isCandidate(this.candidate)) {
                this.notifyOnGranted();
            }

            this.restartLeaderReadinessWatcher();
            LOGGER.debug("New leader is '{}'", this.localLeader);
        }
    }

    protected void notifyOnGranted() {
        LOGGER.debug("Leadership has been granted for '{}'", this.candidate);
        Context context = new LeaderContext(this.candidate, this);
        this.leaderEventPublisher.publishOnGranted(this, context, this.candidate.getRole());

        try {
            this.candidate.onGranted(context);
        } catch (InterruptedException var3) {
            LOGGER.warn(var3.getMessage());
            Thread.currentThread().interrupt();
        }

    }

    protected void notifyOnRevoked() {
        LOGGER.debug("Leadership has been revoked for '{}'", this.candidate);
        Context context = new LeaderContext(this.candidate, this);
        this.leaderEventPublisher.publishOnRevoked(this, context, this.candidate.getRole());
        this.candidate.onRevoked(context);
    }

    protected void notifyOnFailedToAcquire() {
        if (this.leaderProperties.isPublishFailedEvents()) {
            Context context = new LeaderContext(this.candidate, this);
            this.leaderEventPublisher.publishOnFailedToAcquire(this, context, this.candidate.getRole());
        }

    }

    protected void restartLeaderReadinessWatcher() {
        if (this.leaderReadinessWatcher != null) {
            this.leaderReadinessWatcher.stop();
            this.leaderReadinessWatcher = null;
        }

        if (this.localLeader != null && !this.localLeader.isCandidate(this.candidate)) {
            this.leaderReadinessWatcher = this.createPodReadinessWatcher(this.localLeader.getId());
            this.leaderReadinessWatcher.start();
        }

    }

    protected abstract PodReadinessWatcher createPodReadinessWatcher(String localLeaderId);
}