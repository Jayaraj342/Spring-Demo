package com.spring.demo.kubernetes;

public interface PodReadinessWatcher {
    void start();

    void stop();
}