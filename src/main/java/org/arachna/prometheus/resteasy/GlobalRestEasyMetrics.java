package org.arachna.prometheus.resteasy;

import io.prometheus.client.Counter;

import java.util.function.Function;

/**
 * Global counters for requests fulfilled via RESTEasy.
 */
public class GlobalRestEasyMetrics {
    public static Counter REQUESTS;
    public static Counter RESPONSE_5XX;
    public static Counter RESPONSE_4XX;
    public static Counter RESPONSE_3XX;
    public static Counter RESPONSE_2XX;

    static void init(final String prefix) {
        if (REQUESTS == null) {
            Function<String, String> name = s -> {
                return (prefix == null || prefix.length() == 0) ? s : (prefix + "_" + s);
            };

            REQUESTS = Counter.build()
                    .name(name.apply("requests_total")).help("Total tracked requests.").register();
            RESPONSE_5XX = Counter.build().name(name.apply("response_5xx")).help("5xx response count").register();
            RESPONSE_4XX = Counter.build().name(name.apply("response_4xx")).help("4xx response count").register();
            RESPONSE_3XX = Counter.build().name(name.apply("response_3xx")).help("3xx response count").register();
            RESPONSE_2XX = Counter.build().name(name.apply("response_2xx")).help("2xx response count").register();
        }
    }
}
