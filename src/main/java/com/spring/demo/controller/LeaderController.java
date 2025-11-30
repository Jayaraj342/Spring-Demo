package com.spring.demo.controller;

import com.spring.demo.kubernetes.PodAnnotationUpdater;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.leader.Context;
import org.springframework.integration.leader.event.OnGrantedEvent;
import org.springframework.integration.leader.event.OnRevokedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class LeaderController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderController.class);

    private final String host;

    @Value("${spring.cloud.kubernetes.leader.role}")
    private String role;

    private Context context;

    @Autowired
    private KubernetesClient kubernetesClient;

    @Autowired
    private PodAnnotationUpdater podAnnotationUpdater;

    private boolean firstTime = true;

    public LeaderController() throws UnknownHostException {
        this.host = InetAddress.getLocalHost().getHostName();
    }

    /**
     * Return a message whether this instance is a leader or not.
     *
     * @return info
     */
    @GetMapping("/")
    public String getInfo() {
        System.out.printf("==========================> " + System.getenv("HOSTNAME"));// pod identifier
        System.out.printf("==========================> " + System.getenv("KUBERNETES_SERVICE_HOST"));// cluster ip of service api (10.96.0.1) => pod -> kube proxy (cluster ip -> control plane ip) -> server api

        if (this.context == null) {
            return String.format("I am '%s' but I am not a leader of the '%s'", this.host, this.role);
        }

        return String.format("I am '%s' and I am the leader of the '%s'", this.host, this.role);
    }

//    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
    public void printInfo() throws UnknownHostException {
        if (firstTime) {
            System.out.printf("++++++++++++++++++++++++++++++++++++++++++++ Before");
            System.out.printf(
                    kubernetesClient.pods()
                            .withName(InetAddress.getLocalHost().getHostName()).get().getMetadata().getAnnotations().toString()
            );

            podAnnotationUpdater.addInstanceIdAnnotation(UUID.randomUUID().toString());

            System.out.printf("++++++++++++++++++++++++++++++++++++++++++++ After");
            System.out.printf(
                    kubernetesClient.pods()
                            .withName(InetAddress.getLocalHost().getHostName()).get().getMetadata().getAnnotations().toString()
            );

            firstTime = false;
        }

        if (this.context == null) {
            System.out.printf("---I am '%s' but I am not a leader of the '%s'%n", this.host, this.role);
        } else {
            System.out.printf("---I am '%s' and I am the leader of the '%s'%n", this.host, this.role);
        }

        PodList podList = kubernetesClient.pods().list();
        for (Pod pod : podList.getItems()) {
            System.out.println("Incarnation ids for pod : " + pod.getMetadata().getName() + " => " + pod.getMetadata().getAnnotations());
        }
    }

    /**
     * PUT request to try and revoke a leadership of this instance. If the instance is not
     * a leader, leadership cannot be revoked. Thus "HTTP Bad Request" response. If the
     * instance is a leader, it must have a leadership context instance which can be used
     * to give up the leadership.
     *
     * @return info about leadership
     */
    @PutMapping("/")
    public ResponseEntity<String> revokeLeadership() {
        if (this.context == null) {
            String message = String.format("Cannot revoke leadership because '%s' is not a leader", this.host);
            return ResponseEntity.badRequest().body(message);
        }

        this.context.yield();

        String message = String.format("Leadership revoked for '%s'", this.host);
        return ResponseEntity.ok(message);
    }

    /**
     * Handle a notification that this instance has become a leader.
     *
     * @param event on granted event
     */
    @EventListener
    public void handleEvent(OnGrantedEvent event) {
        System.out.printf("'%s' leadership granted%n", event.getRole());
        this.context = event.getContext();
    }

    /**
     * Handle a notification that this instance's leadership has been revoked.
     *
     * @param event on revoked event
     */
    @EventListener
    public void handleEvent(OnRevokedEvent event) {
        System.out.printf("'%s' leadership revoked%n", event.getRole());
        this.context = null;
    }

}
