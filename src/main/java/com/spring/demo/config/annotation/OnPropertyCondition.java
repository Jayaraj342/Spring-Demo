package com.spring.demo.config.annotation;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

public class OnPropertyCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

        Map<String, Object> attrs = metadata.getAnnotationAttributes(ConditionalOnProperty.class.getName());
        if (attrs == null) {
            return false;
        }

        String[] propertyNames = (String[]) attrs.get("value");
        boolean matchIfMissing = (boolean) attrs.get("matchIfMissing");
        String havingValue = (String) attrs.get("havingValue");

        for (String propertyName : propertyNames) {
            String propValue = context.getEnvironment().getProperty(propertyName);

            if (propValue == null) {
                if (!matchIfMissing) {
                    return false;
                }
                // If matchIfMissing==true and property missing, consider match succeeded, continue
            } else {
                if (!propValue.equalsIgnoreCase(havingValue)) {
                    return false;
                }
            }
        }

        return true;
    }
}
