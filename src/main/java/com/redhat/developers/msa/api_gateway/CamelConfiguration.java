/**
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.developers.msa.api_gateway;

import java.util.logging.Logger;

import org.apache.camel.component.hystrix.metrics.servlet.HystrixEventStreamServlet;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.uber.jaeger.metrics.Metrics;
import com.uber.jaeger.metrics.NullStatsReporter;
import com.uber.jaeger.metrics.StatsFactoryImpl;
import com.uber.jaeger.reporters.RemoteReporter;
import com.uber.jaeger.samplers.ProbabilisticSampler;
import com.uber.jaeger.senders.Sender;
import com.uber.jaeger.senders.UdpSender;

import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;

@Configuration
public class CamelConfiguration {
	private static final Logger log = Logger.getLogger(CamelConfiguration.class.getName());

	/**
	 * Bind the Camel servlet at the "/api" context path.
	 */
	@Bean
	public ServletRegistrationBean camelServletRegistrationBean() {
		ServletRegistrationBean mapping = new ServletRegistrationBean();
		mapping.setServlet(new CamelHttpTransportServlet());
		mapping.addUrlMappings("/api/*");
		mapping.setName("CamelServlet");
		mapping.setLoadOnStartup(1);

		return mapping;
	}

	/**
	 * Bind the Hystrix servlet to /hystrix.stream
	 */
	@Bean
	public ServletRegistrationBean hystrixServletRegistrationBean() {
		ServletRegistrationBean mapping = new ServletRegistrationBean();
		mapping.setServlet(new HystrixEventStreamServlet());
		mapping.addUrlMappings("/hystrix.stream");
		mapping.setName("HystrixEventStreamServlet");

		return mapping;
	}

	@Bean
	public Tracer tracer() {
		String jaegerURL = System.getenv("JAEGER_SERVER_HOSTNAME");
		if (jaegerURL != null) {
			log.info("Using Jaeger tracer");
			return jaegerTracer(jaegerURL);
		}

		log.info("Using Noop tracer");
		return NoopTracerFactory.create();
	}

	private Tracer jaegerTracer(String url) {
		Sender sender = new UdpSender(url, 0, 0);
		return new com.uber.jaeger.Tracer.Builder("api-gateway",
				new RemoteReporter(sender, 100, 50, new Metrics(new StatsFactoryImpl(new NullStatsReporter()))),
				new ProbabilisticSampler(1.0)).build();
	}
}
