package com.spring.demo.kubernetes;

import org.springframework.integration.leader.Candidate;
import org.springframework.integration.leader.Context;

public class LeaderContext implements Context {
    private final Candidate candidate;
    private final LeadershipController leadershipController;

    public LeaderContext(Candidate candidate, LeadershipController leadershipController) {
        this.candidate = candidate;
        this.leadershipController = leadershipController;
    }

    public boolean isLeader() {
        return this.leadershipController.getLocalLeader().filter((l) -> {
            return l.isCandidate(this.candidate);
        }).isPresent();
    }

    public void yield() {
        this.leadershipController.revoke();
    }
}
