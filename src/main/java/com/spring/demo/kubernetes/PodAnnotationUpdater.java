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

            System.out.println("namespace +++++++++++++++" + namespace);

            // Use a more appropriate label
            final String labelValue = "saas-spring";

            System.out.println("client.optionalPod().inNamespace(namespace)" + client.pods().inNamespace(namespace).list());

            // Find the specific pod using the most efficient server-side filter possible
            // and then client-side filtering for the exact name if necessary.
            String labelKey = "app";
            var optionalPod = client.pods()
                    .inNamespace(namespace)
                    .withLabel(labelKey, labelValue)
                    .list()
                    .getItems()
                    .stream().filter(pod -> pod.getMetadata().getName().equals(this.podName))
                    .findFirst();


            if (optionalPod.isPresent()) {

                System.out.println("Found pod: " + this.podName + ". Proceeding to edit.");

                // The .edit() function automatically handles the PUT request to the API server
                // after the lambda function returns the modified 'pod' object.
                client.pods()
                        .inNamespace(namespace)
                        .withName(podName) // Use withName for direct access for the edit operation
                        .edit(pod -> {
                            Map<String, String> annotations = pod.getMetadata().getAnnotations();
                            annotations.put(annotationKey, instanceId);
                            System.out.println("Setting annotation " + annotationKey + "=" + instanceId);

                            // Return the modified pod object
                            return pod;
                        });

                System.out.println("Annotation successfully updated on pod: " + podName);
            } else {
                System.err.println("Error: Pod named '" + podName +
                        "' not found in namespace '" + namespace +
                        "' with label '" + labelValue + "'.");
            }
        } catch (Exception e) {
            System.err.println("Failed to add annotation to pod " + this.podName);
            e.printStackTrace();
        }
    }
}
