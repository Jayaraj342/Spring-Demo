package com.spring.demo.config;

import com.spring.demo.config.annotation.ConditionalOnCloudPlatform;
import com.spring.demo.config.annotation.ConditionalOnProperty;
import com.spring.demo.config.annotation.InfaCloudPlatform;
import com.spring.demo.kubernetes.Fabric8LeaderRecordWatcher;
import com.spring.demo.kubernetes.Fabric8LeadershipController;
import com.spring.demo.kubernetes.Fabric8PodReadinessWatcher;
import com.spring.demo.kubernetes.LeaderInitiator;
import com.spring.demo.kubernetes.LeaderProperties;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.leader.Candidate;
import org.springframework.integration.leader.DefaultCandidate;
import org.springframework.integration.leader.event.DefaultLeaderEventPublisher;
import org.springframework.integration.leader.event.LeaderEventPublisher;

import java.net.Inet4Address;
import java.net.UnknownHostException;

@ConditionalOnCloudPlatform(InfaCloudPlatform.KUBERNETES)
@Configuration
@ConditionalOnProperty(
        value = {"spring.cloud.kubernetes.leader.enabled"},
        matchIfMissing = true
)
public class Fabric8LeaderElectionConfig {

    @Bean
    public KubernetesClient kubernetesClient() {
        KubernetesClient kubernetesClient = new KubernetesClientBuilder().build();
        int pods = kubernetesClient.pods().list().getItems().size();
        System.out.println("===================> kubernetesClient.pods().list().getItems().size() : " + pods);
        for (int i = 0; i < pods; i++) {
            System.out.println("===================> pod : " + i +
                    kubernetesClient.pods().list().getItems().get(i).getMetadata().getName()
            );
        }

        return kubernetesClient;
    }

    @Bean
    public Candidate candidate(
            @Value("${spring.cloud.kubernetes.leader.role:world-spring}"
            ) String role) throws UnknownHostException {
        String id = Inet4Address.getLocalHost().getHostName();
        return new DefaultCandidate(id, role);
    }

    @Bean
    public LeaderProperties leaderProperties() {
        return new LeaderProperties();
    }

    @Bean
    public Fabric8LeadershipController leadershipController(Candidate candidate, LeaderProperties leaderProperties, LeaderEventPublisher leaderEventPublisher, KubernetesClient kubernetesClient) {
        return new Fabric8LeadershipController(candidate, leaderProperties, leaderEventPublisher, kubernetesClient);
    }

    @Bean
    public Fabric8LeaderRecordWatcher leaderRecordWatcher(LeaderProperties leaderProperties, Fabric8LeadershipController fabric8LeadershipController, KubernetesClient kubernetesClient) {
        return new Fabric8LeaderRecordWatcher(leaderProperties, fabric8LeadershipController, kubernetesClient);
    }

    @Bean
    public Fabric8PodReadinessWatcher hostPodWatcher(Candidate candidate, KubernetesClient kubernetesClient, Fabric8LeadershipController fabric8LeadershipController) {
        return new Fabric8PodReadinessWatcher(candidate.getId(), kubernetesClient, fabric8LeadershipController);
    }

    @Bean(
            destroyMethod = "stop"
    )
    public LeaderInitiator leaderInitiator(LeaderProperties leaderProperties, Fabric8LeadershipController fabric8LeadershipController, Fabric8LeaderRecordWatcher fabric8LeaderRecordWatcher, Fabric8PodReadinessWatcher hostPodWatcher) {
        return new LeaderInitiator(leaderProperties, fabric8LeadershipController, fabric8LeaderRecordWatcher, hostPodWatcher);
    }

    @Bean
    public LeaderEventPublisher defaultLeaderEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new DefaultLeaderEventPublisher(applicationEventPublisher);
    }
}
