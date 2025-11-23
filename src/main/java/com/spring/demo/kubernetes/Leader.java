package com.spring.demo.kubernetes;

import org.springframework.integration.leader.Candidate;

import java.util.Objects;

public class Leader {
    private final String role;
    private final String id;

    public Leader(String role, String id) {
        this.role = role;
        this.id = id;
    }

    public String getRole() {
        return this.role;
    }

    public String getId() {
        return this.id;
    }

    public boolean isCandidate(Candidate candidate) {
        if (candidate == null) {
            return false;
        } else {
            return Objects.equals(this.role, candidate.getRole()) && Objects.equals(this.id, candidate.getId());
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            Leader leader = (Leader) o;
            return Objects.equals(this.role, leader.role) && Objects.equals(this.id, leader.id);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.role, this.id});
    }

    public String toString() {
        return String.format("Leader{role='%s', id='%s'}", this.role, this.id);
    }
}

