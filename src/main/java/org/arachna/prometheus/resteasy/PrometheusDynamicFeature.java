package org.arachna.prometheus.resteasy;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

@Provider
public class PrometheusDynamicFeature implements DynamicFeature {
    private final boolean profileAll;
    private final String prefix;

    public PrometheusDynamicFeature() {
        this(profileAll());
    }

    private static boolean profileAll() {
        String val = System.getProperty("prometheus.resteasy.all", System.getenv("PROMETHEUS_RESTEASY_ALL"));
        return val == null ? true : Boolean.valueOf(val);
    }

    public PrometheusDynamicFeature(boolean profileAll) {
        this.prefix = System.getProperty("prometheus.resteasy.prefix", System.getenv("PROMETHEUS_RESTEASY_PREFIX") );

        GlobalRestEasyMetrics.init(prefix);

        this.profileAll = profileAll;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Prometheus annotation = resourceInfo.getResourceClass().getAnnotation(Prometheus.class);
        if (annotation != null || profileAll) {
            context.register(new PrometheusRestEasyFilter(resourceInfo, prefix, annotation));
        }
    }
}
