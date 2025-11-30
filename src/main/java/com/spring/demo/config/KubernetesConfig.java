package com.spring.demo.config;

import com.spring.demo.config.annotation.ConditionalOnCloudPlatform;
import com.spring.demo.config.annotation.ConditionalOnProperty;
import com.spring.demo.config.annotation.InfaCloudPlatform;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.extended.leaderelection.LeaderCallbacks;
import io.fabric8.kubernetes.client.extended.leaderelection.LeaderElectionConfig;
import io.fabric8.kubernetes.client.extended.leaderelection.LeaderElectionConfigBuilder;
import io.fabric8.kubernetes.client.extended.leaderelection.LeaderElector;
import io.fabric8.kubernetes.client.extended.leaderelection.resourcelock.LeaseLock;
import io.fabric8.kubernetes.client.extended.leaderelection.resourcelock.Lock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.function.Consumer;

@ConditionalOnCloudPlatform(InfaCloudPlatform.KUBERNETES)
@Configuration
@ConditionalOnProperty(
        value = {"spring.cloud.kubernetes.leader.enabled"},
        matchIfMissing = true
)
public class KubernetesConfig {

    // These values can be placed in your application.properties file
    @Value("${kubernetes.leader-election.namespace:default}")
    private String namespace;

    @Value("${kubernetes.leader-election.name:leaders-of-the-future}")
    private String lockName;

    @Bean
    public Lock leaderElectionLock() throws UnknownHostException {
        // We use LeaseLock which is the modern and recommended approach over ConfigMapLock
        return new LeaseLock(namespace, lockName, Inet4Address.getLocalHost().getHostName());
    }

    @Bean
    public LeaderCallbacks fabric8LeaderCallbacks() {

        Runnable onStartLeading = () -> {
            System.out.println(">>>> I just became leader! Starting critical tasks. <<<<");
        };

        Runnable onStopLeading = () -> {
            System.out.println("<<<< I lost leadership. Stopping critical tasks. >>>>");
        };

        Consumer<String> onNewLeader = (leaderIdentity) -> {
            System.out.println("New leader elected: " + leaderIdentity);
        };

        return new LeaderCallbacks(onStartLeading, onStopLeading, onNewLeader);
    }

    // TODO : destroyMethod to be looked
    @Bean
    public LeaderElector leaderElector(KubernetesClient client, Lock lock, LeaderCallbacks leaderCallback) {
        LeaderElectionConfig config = new LeaderElectionConfigBuilder()
                .withName("SpringBoot Leader Election Configuration")
                .withLeaseDuration(Duration.ofSeconds(15L)) // How long the lease is valid
                .withRenewDeadline(Duration.ofSeconds(10L)) // Max time for renewal attempts
                .withRetryPeriod(Duration.ofSeconds(2L))    // Time between retries
                .withLock(lock)
                // Injecting a separate Spring component for callbacks
                .withLeaderCallbacks(leaderCallback)
                .build();

        LeaderElector elector = client.leaderElector()
                .withConfig(config)
                .build();

        // Start the election process immediately when the application context loads
        System.out.println("starting ----------------------------------------------");
        elector.start();
        System.out.println("started ----------------------------------------------");
        return elector;
    }
}
