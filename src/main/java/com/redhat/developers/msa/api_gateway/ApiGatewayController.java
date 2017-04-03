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

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.redhat.developers.msa.api_gateway.feign.GenericFeignClient;

import io.opentracing.contrib.spanmanager.DefaultSpanManager;
import io.opentracing.contrib.spanmanager.SpanManager;
import io.swagger.annotations.ApiOperation;

@RestController
public class ApiGatewayController {

    @Autowired
    private List<GenericFeignClient> genericFeignClients;

    @CrossOrigin
    @RequestMapping(method = RequestMethod.GET, value = "/api/gateway", produces = "application/json")
    @ApiOperation("Invoke all microservices in parallel")
    public List<String> api() {
        SpanManager.ManagedSpan serverSpan = DefaultSpanManager.getInstance().current();
        return genericFeignClients
                .stream()
                .parallel()
                // We have to set serverSpan because this executor service is not binding server span to new thread
                .map(genericFeignClient -> {
                    DefaultSpanManager.getInstance().activate(serverSpan.getSpan());
                    return genericFeignClient.invokeService();
                })
                .collect(Collectors.toList());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/health")
    @ApiOperation("Used to verify the health of the service")
    public String health() {
        return "I'm ok";
    }
}
