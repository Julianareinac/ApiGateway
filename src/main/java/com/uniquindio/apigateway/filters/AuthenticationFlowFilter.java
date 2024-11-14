package com.uniquindio.apigateway.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniquindio.apigateway.model.AuthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;


@Component
public class AuthenticationFlowFilter extends AbstractGatewayFilterFactory<AuthenticationFlowFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFlowFilter.class);

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    // Caché en memoria para el token de autenticación
    private static final ConcurrentHashMap<String, String> tokenCache = new ConcurrentHashMap<>();


    @Value("${service.auth.url}")
    private String authServiceUrl;

    @Value("${service.profile.url}")
    private String profileServiceUrl;

    public static class Config {

    }

    public AuthenticationFlowFilter(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
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
                                .uri(authServiceUrl)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(requestBody)
                                .retrieve()
                                .bodyToMono(AuthResponse.class)
                                .flatMap(authResponse -> {
                                    logger.info("Authentication response: {}", authResponse);
                                    logger.info("Captured Token: {}", authResponse.getToken());

                                    if (authResponse.getToken() == null) {
                                        logger.error("Received token is null");
                                    }
                                    // Almacenar el token en caché
                                    tokenCache.put("authToken", authResponse.getToken());
                                    logger.info("Token almacenado en caché: {}", authResponse.getToken());


                                    return webClientBuilder.build()
                                            .get()
                                            .uri(profileServiceUrl)
                                            .header("Authorization", "Bearer " + authResponse.getToken())
                                            .retrieve()
                                            .bodyToMono(String.class)
                                            .flatMap(profileResponse -> {
                                                ServerHttpResponse response = exchange.getResponse();
                                                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                                                DataBuffer buffer = response.bufferFactory().wrap(profileResponse.getBytes());
                                                return response.writeWith(Mono.just(buffer));
                                            });
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

    // Método estático para acceder al token almacenado
    public static String getCachedToken() {
        return tokenCache.get("authToken");
    }
}