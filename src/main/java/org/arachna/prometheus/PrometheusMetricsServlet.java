package org.arachna.prometheus;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.prometheus.client.hotspot.DefaultExports;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/metrics")
public class PrometheusMetricsServlet extends HttpServlet {
    static {
        DefaultExports.initialize();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-Type", TextFormat.CONTENT_TYPE_004);

        try (Writer writer = resp.getWriter()) {
            TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
            writer.flush();
        }
        catch(IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getLocalizedMessage(),e);
        }
    }
}
