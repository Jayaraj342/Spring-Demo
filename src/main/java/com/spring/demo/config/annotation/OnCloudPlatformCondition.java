package com.spring.demo.config.annotation;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

public class OnCloudPlatformCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnCloudPlatform.class.getName());
        if (attributes == null) {
            return false;
        }

        InfaCloudPlatform requiredPlatform = (InfaCloudPlatform) attributes.get("value");

        // Logic to detect Kubernetes environment:
        // (Customize this logic based on your needs)
        // For example, check for environment variables or system properties typical to Kubernetes:
        boolean isKubernetes = System.getenv("KUBERNETES_SERVICE_HOST") != null;

        if (requiredPlatform == InfaCloudPlatform.KUBERNETES) {
            return isKubernetes;
        }

        return false;
    }
}