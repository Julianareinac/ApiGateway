package com.uniquindio.apigateway.filters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uniquindio.apigateway.model.AuthRequest;
import com.uniquindio.apigateway.model.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class UpdateProfileFilter extends AbstractGatewayFilterFactory<UpdateProfileFilter.Config> {


    @Value("${service.profile.url}")
    private String profileServiceUrl;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFlowFilter.class);



    public static class Config {
    }

    public UpdateProfileFilter(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            // Obtener el token almacenado en caché
            String cachedToken = AuthenticationFlowFilter.getCachedToken();
            logger.info("token" + cachedToken);

            if (cachedToken == null) {
                return Mono.error(new RuntimeException("Token no encontrado en caché"));
            }

            return exchange.getRequest().getBody()
                    .map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        return new String(bytes);
                    })
                    .collectList()
                    .flatMap(bodyParts -> {
                        String requestBody = String.join("", bodyParts);
                        logger.info("requestBody" + requestBody);


                            return webClientBuilder.build()
                                    .put()
                                    .uri(profileServiceUrl)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + cachedToken)
                                    .bodyValue(requestBody)
                                    .retrieve()
                                    .bodyToMono(String.class)
                                    .flatMap(profileResponse -> {
                                        ServerHttpResponse response = exchange.getResponse();
                                        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                                        DataBuffer buffer = response.bufferFactory().wrap(profileResponse.getBytes());
                                        return response.writeWith(Mono.just(buffer));
                                    });

                    });
        };
    }
}
