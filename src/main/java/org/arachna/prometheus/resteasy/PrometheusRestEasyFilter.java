package org.arachna.prometheus.resteasy;

import io.prometheus.client.Histogram;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.*;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.arachna.prometheus.resteasy.GlobalRestEasyMetrics.REQUESTS;
import static org.arachna.prometheus.resteasy.GlobalRestEasyMetrics.RESPONSE_2XX;
import static org.arachna.prometheus.resteasy.GlobalRestEasyMetrics.RESPONSE_3XX;
import static org.arachna.prometheus.resteasy.GlobalRestEasyMetrics.RESPONSE_4XX;
import static org.arachna.prometheus.resteasy.GlobalRestEasyMetrics.RESPONSE_5XX;

/**
 * Filter for resources exposed via RESTeasy.
 */
@Priority(Priorities.HEADER_DECORATOR)
public class PrometheusRestEasyFilter implements ContainerRequestFilter, ContainerResponseFilter {


    private static final String TRACKER_TIMER = "prometheus.timer";

    protected final ResourceInfo resourceInfo;
    protected String prefix = "";
    private final Prometheus annotation;
    private Histogram tracker;

    /**
     * Registers a filter specifically for the defined method.
     *
     * @param resourceInfo - the resource (uri to class + method) we are registering this filter for
     * @param prefix - the prefix we should apply to all metrics (if any)
     * @param annotation - one if it exists
     */
    public PrometheusRestEasyFilter(ResourceInfo resourceInfo, String prefix, Prometheus annotation) {
        this.resourceInfo = resourceInfo;
        this.prefix = prefix;
        this.annotation = annotation;

        buildTimerFromAnnotation(annotation);
    }

    /**
     * if the annotation is fully specified, use it.
     *
     * @param annotation - provides us a name and help
     */
    private void buildTimerFromAnnotation(Prometheus annotation) {
        if (annotation != null && annotation.help().length() > 0 && annotation.name().length() > 0) {
            tracker = Histogram.build().name(annotation.name()).help(annotation.help()).register();
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (tracker == null) { // we only need to do this once
            buildTracker(requestContext);
        }

        if (tracker != null) {
            Histogram.Timer timer = tracker.startTimer();
            requestContext.setProperty(TRACKER_TIMER, timer);
        }
    }

    private void buildTracker(ContainerRequestContext requestContext) {
        String path = annotation == null ? "" : annotation.help();

        if (path.length() == 0) {
            // this won't change from request to request
            MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();

            path = path(requestContext.getUriInfo().getRequestUri());

            for (Map.Entry<String, List<String>> entry: pathParameters.entrySet()) {
                final String originalPathFragment = String.format("{%s}", entry.getKey());

                for (String currentPathFragment: entry.getValue()) {
                    path = path.replace(currentPathFragment, originalPathFragment);
                }
            }
        }

        String name = annotation == null ? "" : annotation.name();

        if (name.length() == 0) {
            // we cannot use the class name as it is always a proxy
            name = resourceInfo.getResourceMethod().getName();
        }

        if (prefix != null && prefix.length() > 0) {
            name = prefix + "_" + name;
        }

        tracker = Histogram.build().name(name).help(path).register();
    }

    /**
     * Returns path of given URI. If the first character of path is '/' then it is removed.
     *
     * @author Pavol Loffay
     * @param uri to convert
     * @return path or null
     */
    public static String path(URI uri) {
        String path = uri.getPath();
        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }

        return path;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        Histogram.Timer timer = Histogram.Timer.class.cast(requestContext.getProperty(TRACKER_TIMER));

        if (timer != null) {
            timer.observeDuration();
        }

        if (responseContext.getStatus() >= 500) {
            RESPONSE_5XX.inc();
        } else if (responseContext.getStatus() >= 400) {
            RESPONSE_4XX.inc();
        } else if (responseContext.getStatus() >= 300) {
            RESPONSE_3XX.inc();
        } else if (responseContext.getStatus() >= 200) {
            RESPONSE_2XX.inc();
        }
    }

    /*
    private static class PrometheusHistogramListener implements RequestEventListener {
        private static ConcurrentHashMap<String, Histogram> histograms = new ConcurrentHashMap<>();
        private static ConcurrentHashMap<Method, Method> ignored = new ConcurrentHashMap<>();
        private Histogram.Timer timer;
        private final String prefix;

        private PrometheusHistogramListener(String prefix) {
            this.prefix = prefix;
        }

        private void resourceStart(RequestEvent matched) {
            Method method = matched.getUriInfo().getMatchedResourceMethod().getInvocable().getHandlingMethod();

            if (ignored.get(method) != null) {
                return; // seen it before, its ignored, lets get outta here
            }

            Prometheus instrument = method.getAnnotation(Prometheus.class);

            if (instrument != null) {
                Histogram tracker = histograms.get(instrument.name());

                if (tracker == null) { // we don't know about it either way
                    tracker = Histogram.build().name(prefix + instrument.name()).help(instrument.help()).register();
                    histograms.put(instrument.name(), tracker);
                }

                timer = tracker.startTimer();
                REQUESTS.inc();

            } else {
                ignored.put(method, method);
            }
        }

        @Override
        public void onEvent(RequestEvent matched) {
            if (matched.getType() == RequestEvent.Type.RESOURCE_METHOD_START ) {
                resourceStart(matched);
            } else if (matched.getType() == RequestEvent.Type.RESOURCE_METHOD_FINISHED && timer != null) {
                timer.observeDuration();
                timer = null;
            }
        }
    }
    */
}
