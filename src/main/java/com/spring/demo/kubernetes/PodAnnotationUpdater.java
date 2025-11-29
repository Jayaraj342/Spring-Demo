package com.spring.demo.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

@Component
public class PodAnnotationUpdater {

    private final KubernetesClient client;
    private final String podName;

    public PodAnnotationUpdater(KubernetesClient client) throws UnknownHostException {
        this.client = client;
        // Get the current pod name from the hostname environment variable
        this.podName = InetAddress.getLocalHost().getHostName();
    }

    public void addInstanceIdAnnotation(String instanceId) {
        final String annotationKey = "incarnationId";

        try {
            // make this one time call
            String namespace = client.pods().withName(InetAddress.getLocalHost().getHostName()).get().getMetadata().getNamespace();

            client.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .edit(p -> {
                        Map<String, String> annotations = p.getMetadata().getAnnotations();
                        annotations.put(annotationKey, instanceId);
                        // The 'p' object is automatically applied/replaced when edit() finishes
                        return p;
                    });
            System.out.println("Successfully added annotation " + annotationKey + "=" + instanceId + " to pod " + this.podName);
        } catch (Exception e) {
            System.err.println("Failed to add annotation to pod " + this.podName);
            e.printStackTrace();
        }
    }
}
