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

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.component.hystrix.metrics.servlet.HystrixEventStreamServlet;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.zipkin.ZipkinTracer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.kristofa.brave.EmptySpanCollector;
import com.github.kristofa.brave.EmptySpanCollectorMetricsHandler;
import com.github.kristofa.brave.http.HttpSpanCollector;

@Configuration
public class CamelConfiguration {

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

    /**
     * Configure Zipkin traces
     */
    @Bean
    ZipkinTracer zipkinEventNotifier(CamelContext camelContext) {
        ZipkinTracer zipkin = new ZipkinTracer();

        // Map Camel endpoints to names
        Map<String, String> clientConfig = new HashMap<>();
        clientConfig.put("http4:hola:*", "hola");
        clientConfig.put("http4:ola:*", "ola");
        clientConfig.put("http4:bonjour:*", "bonjour");

        zipkin.setClientServiceMappings(clientConfig);

        // Map consumer endpoints to names
        Map<String, String> serverConfig = new HashMap<>();
        serverConfig.put("servlet:*", "api-gateway");
        serverConfig.put("direct:ola", "ola");

        zipkin.setServerServiceMappings(serverConfig);

        // Tracer configuration
        zipkin.setServiceName("api-gateway");
        zipkin.setIncludeMessageBody(true);
        zipkin.setIncludeMessageBodyStreams(true);
        String zipkinUrl = System.getenv("ZIPKIN_SERVER_URL");
        if (zipkinUrl != null) {
            zipkin.setSpanCollector(HttpSpanCollector.create(zipkinUrl, new EmptySpanCollectorMetricsHandler()));
        } else {
            zipkin.setSpanCollector(new EmptySpanCollector());
        }

        // register the bean into CamelContext
        zipkin.init(camelContext);

        return zipkin;
    }

}
