package com.example.servicea.client;

import com.example.servicea.model.BatchDataResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class HttpDataClient {

    private final WebClient webClient;

    public HttpDataClient(@Value("${service-b.http-url}") String serviceBUrl) {
        this.webClient = WebClient.builder()
            .baseUrl(serviceBUrl)
            .build();
    }

    public Mono<BatchDataResponse> getBatchData(int count) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/data/generate")
                .queryParam("count", count)
                .build())
            .retrieve()
            .bodyToMono(BatchDataResponse.class);
    }
}