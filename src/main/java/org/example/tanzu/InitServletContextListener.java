package org.example.tanzu;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmCompilationMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.postgresql.ds.PGPoolingDataSource;

@WebListener
public class InitServletContextListener implements ServletContextListener {
	@Override
	@SuppressWarnings("deprecrted")
	public void contextInitialized(ServletContextEvent event) {
		final ServletContext servletContext = event.getServletContext();
		this.initDataSource(servletContext);
		this.initMeterRegistry(servletContext);
	}

	void initDataSource(ServletContext servletContext) {
		final String jdbcUrl = System.getenv("JDBC_URL");
		final String jdbcUrlReadOnly = System.getenv("JDBC_URL_READ_ONLY");
		if (jdbcUrl != null) {
			final PGPoolingDataSource dataSource = new PGPoolingDataSource();
			dataSource.setMaxConnections(32);
			dataSource.setInitialConnections(8);
			dataSource.setUrl(jdbcUrl);
			servletContext.setAttribute("dataSource", dataSource);
			if (jdbcUrlReadOnly != null) {
				final PGPoolingDataSource dataSourceReadOnly = new PGPoolingDataSource();
				dataSourceReadOnly.setUrl(jdbcUrlReadOnly);
				servletContext.setAttribute("dataSourceReadOnly", dataSourceReadOnly);
			}
			else {
				servletContext.setAttribute("dataSourceReadOnly", dataSource);
			}
			try (final Connection connection = dataSource.getConnection()) {
				try (final PreparedStatement prepareStatement = connection.prepareStatement("CREATE TABLE access_log("
						+ " id SERIAL PRIMARY KEY, "
						+ " ip VARCHAR(128) NOT NULL,"
						+ " created_at TIMESTAMP WITH TIME zone DEFAULT now());")) {
					prepareStatement.execute();
				}
				catch (SQLException ignored) {
				}
			}
			catch (SQLException e) {
				e.printStackTrace();
				servletContext.setAttribute("dataSource", null);
				servletContext.setAttribute("dataSourceReadOnly", null);
			}
		}
	}

	void initMeterRegistry(ServletContext servletContext) {
		final MeterRegistry meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
		new JvmGcMetrics().bindTo(meterRegistry);
		new JvmHeapPressureMetrics().bindTo(meterRegistry);
		new JvmMemoryMetrics().bindTo(meterRegistry);
		new JvmThreadMetrics().bindTo(meterRegistry);
		new JvmCompilationMetrics().bindTo(meterRegistry);
		new ClassLoaderMetrics().bindTo(meterRegistry);
		new UptimeMetrics().bindTo(meterRegistry);
		new ProcessorMetrics().bindTo(meterRegistry);
		new FileDescriptorMetrics().bindTo(meterRegistry);
		servletContext.setAttribute("meterRegistry", meterRegistry);
	}
}
