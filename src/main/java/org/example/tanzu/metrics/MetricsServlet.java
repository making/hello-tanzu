package org.example.tanzu.metrics;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.common.TextFormat;

@WebServlet(name = "metrics", urlPatterns = "/metrics")
public class MetricsServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		final PrometheusMeterRegistry meterRegistry = (PrometheusMeterRegistry) this.getServletContext().getAttribute("meterRegistry");
		res.setContentType(TextFormat.CONTENT_TYPE_004);
		PrintWriter pw = res.getWriter();
		pw.print(meterRegistry.scrape());
		pw.flush();
	}
}