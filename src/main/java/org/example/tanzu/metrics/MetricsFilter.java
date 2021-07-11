package org.example.tanzu.metrics;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Sample;
import io.micrometer.core.instrument.binder.http.DefaultHttpServletRequestTagsProvider;
import io.micrometer.core.instrument.binder.http.HttpRequestTags;
import io.micrometer.core.instrument.binder.http.HttpServletRequestTagsProvider;


@WebFilter(filterName = "metrics", urlPatterns = "/*")
public class MetricsFilter implements Filter {
	public static final String ALREADY_FILTERED_NAME = MetricsFilter.class.getName() + ".FILTERED";

	private final HttpServletRequestTagsProvider tagsProvider = new DefaultHttpServletRequestTagsProvider();

	private MeterRegistry meterRegistry;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.meterRegistry = (MeterRegistry) filterConfig.getServletContext().getAttribute("meterRegistry");
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		if (servletRequest.getAttribute(ALREADY_FILTERED_NAME) == null) {
			try {
				servletRequest.setAttribute(ALREADY_FILTERED_NAME, Boolean.TRUE);
				this.doFilterInternal((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, filterChain);
			}
			finally {
				servletRequest.removeAttribute(ALREADY_FILTERED_NAME);
			}
		}
		else {
			filterChain.doFilter(servletRequest, servletResponse);
		}
	}

	void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		final Sample sample = Timer.start(this.meterRegistry);
		try {
			filterChain.doFilter(request, response);
			final Timer timer = this.timerBuilder(request, response, null).register(this.meterRegistry);
			sample.stop(timer);
		}
		catch (IOException | RuntimeException | ServletException e) {
			final Timer timer = this.timerBuilder(request, response, e).register(this.meterRegistry);
			sample.stop(timer);
			throw e;
		}
	}

	Timer.Builder timerBuilder(HttpServletRequest request, HttpServletResponse response, Throwable exception) {
		return Timer.builder("http.server.requests")
				.tags(this.tagsProvider.getTags(request, response))
				.tags(Collections.singleton(HttpRequestTags.exception(exception)))
				.tag("uri", request.getServletPath());
	}
}