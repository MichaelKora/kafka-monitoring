package de.tuberlin.dos.monitoring.consumer;

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

public class LatencyExporter {

	private static final Logger log = LoggerFactory.getLogger(LatencyExporter.class);
	private PrometheusMeterRegistry prometheusRegistry;

	public static LatencyExporter create() {
		LatencyExporter latencyExporter = new LatencyExporter();
		latencyExporter.prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
		latencyExporter.prometheusRegistry.config().commonTags("application", "consumerapp");
		latencyExporter.wirePrometheus();
		return latencyExporter;
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
