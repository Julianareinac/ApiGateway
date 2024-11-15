package com.uniquindio.apigateway.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniquindio.apigateway.model.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;


@Component
public class RegisterFlowFilter extends AbstractGatewayFilterFactory<RegisterFlowFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(RegisterFlowFilter.class);

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;



    @Value("${register.service,url}")
    private String registerServiceUrl;


    public static class Config {

    }

    public RegisterFlowFilter(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return exchange.getRequest().getBody()
                    .map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        return new String(bytes);
                    })
                    .collectList()
                    .flatMap(bodyParts -> {
                        String requestBody = String.join("", bodyParts);

                        return webClientBuilder.build()
                                .post()
                                .uri(registerServiceUrl)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(requestBody)
                                .retrieve()
                                .bodyToMono(AuthResponse.class)
                                .flatMap(authResponse -> {
                                    // Retornar el token al cliente
                                    ServerHttpResponse response = exchange.getResponse();
                                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                                    String responseBody = "{\"token\":\"" + authResponse.getToken() + "\"}";
                                    DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes());
                                    return response.writeWith(Mono.just(buffer));
                                });
                    })
                    .onErrorResume(error -> {
                        ServerHttpResponse response = exchange.getResponse();
                        response.setStatusCode(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
                        String errorMessage = "Error during authentication flow: " + error.getMessage();
                        DataBuffer buffer = response.bufferFactory().wrap(errorMessage.getBytes());
                        return response.writeWith(Mono.just(buffer));
                    });
        };
    }

}