package com.uniquindio.apigateway.configs;

import com.uniquindio.apigateway.filters.AuthenticationFlowFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class GatewayFilterConfig {

    @Autowired
    private AuthenticationFlowFilter authFlowFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("login-flow", r -> r.path("/login")
                        .filters(f -> f.filter(authFlowFilter.apply(new AuthenticationFlowFilter.Config())))
                        .uri("no://op"))
                .build();
    }
}