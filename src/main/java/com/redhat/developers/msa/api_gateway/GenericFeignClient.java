/**
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc. and/or its affiliates, and individual
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

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.redhat.developers.msa.api_gateway.tracing.ApiGatewayHttpRequestInterceptor;
import com.redhat.developers.msa.api_gateway.tracing.ApiGatewayHttpResponseInterceptor;

import feign.Logger;
import feign.Logger.Level;
import feign.httpclient.ApacheHttpClient;
import feign.hystrix.HystrixFeign;
import io.opentracing.Span;

/**
 * This class constructs a Feign Client to be invoked
 *
 */
public abstract class GenericFeignClient<T> {

    private String serviceName;

    private Class<T> classType;

    private T fallBack;

    /**
     * We need the following information to instantiate a FeignClient
     * 
     * @param classType Service that will be invoked
     * @param serviceName the name of the service. It will be used in the hostname and in zipking tracing
     * @param fallback the fallback implementation
     */
    public GenericFeignClient(Class<T> classType, String serviceName, T fallback) {
        this.classType = classType;
        this.serviceName = serviceName;
        this.fallBack = fallback;
    }

    /**
     * This should be implemented to call each service interface using the original {@link Span}
     * 
     * @param serverSpan The original {@link Span} received from ZipKin
     * @return Return for each endpoint
     */
    public abstract String invokeService(Span serverSpan);

    /**
     * This is were the "magic" happens: it creates a Feign, which is a proxy interface for remote calling a REST endpoint with
     * Hystrix fallback support.
     * 
     * @param - The original ServerSpan
     * 
     * @return The feign pointing to the service URL and with Hystrix fallback.
     */
    protected T createFeign(Span serverSpan) {
        final CloseableHttpClient httpclient =
            HttpClients.custom()
                .addInterceptorFirst(new ApiGatewayHttpRequestInterceptor(serverSpan))
                .addInterceptorFirst(new ApiGatewayHttpResponseInterceptor(serverSpan))
                .build();

        String url = System.getenv(String.format("%s_SERVER_URL", serviceName.toUpperCase()));
        if (null == url || url.isEmpty()) {
            String host = System.getenv(String.format("%s_SERVICE_HOST", serviceName.toUpperCase()));
            String port = System.getenv(String.format("%s_SERVICE_PORT", serviceName.toUpperCase()));
            if (null == host) {
                url = String.format("http://%s:8080/", serviceName);
            } else {
                url = String.format("http://%s:%s", host, port);
            }
        }

        return HystrixFeign.builder()
            .client(new ApacheHttpClient(httpclient))
            .logger(new Logger.ErrorLogger()).logLevel(Level.BASIC)
            .target(classType, url, fallBack);
    }

}
