package de.tuberlin.dos.monitoring.consumer.metrics;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsExporter {

	private static final String[] TAGS = {"application", "consumerapp"};
	private static final Logger log = LoggerFactory.getLogger(MetricsExporter.class);
	private PrometheusMeterRegistry prometheusRegistry;

	public static MetricsExporter create() {
		MetricsExporter metricsExporter = new MetricsExporter();
		metricsExporter.prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
		metricsExporter.prometheusRegistry.config().commonTags(TAGS);
		metricsExporter.wirePrometheus();
		return metricsExporter;
	}

	public Timer getMsgLatencyTimer(String appId) {
		log.info("Reporting msg latency with appId %s".formatted(appId));
		return this.prometheusRegistry.timer("message.latency", Tags.of("appId", "consumerapp-%s".formatted(appId)));
	}

	private void wirePrometheus() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
			server.createContext("/metrics", httpExchange -> {
				String response = prometheusRegistry.scrape();
				httpExchange.sendResponseHeaders(200, response.getBytes().length);
				try (OutputStream os = httpExchange.getResponseBody()) {
					os.write(response.getBytes());
				}
			});
			new Thread(server::start).start();
		} catch (IOException e) {
			throw new RuntimeException("Could not expose metrics exporter endpoint.", e);
		}
	}

}
