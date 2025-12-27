package com.example.servicea.client;

import com.example.servicea.model.BatchDataResponse;
import com.example.servicea.model.ServerPerformanceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HttpDataClient {

    private final WebClient webClient;

    public HttpDataClient(@Value("${service-b.http-url}") String serviceBUrl) {
        this.webClient = WebClient.builder()
            .baseUrl(serviceBUrl)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
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

    public Mono<Map<String, List<ServerPerformanceMetrics>>> getServerMetrics() {
        return webClient.get()
            .uri("/api/data/metrics")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, List<ServerPerformanceMetrics>>>() {});
    }

    public Mono<Void> clearServerMetrics() {
        return webClient.delete()
            .uri("/api/data/metrics")
            .retrieve()
            .bodyToMono(Void.class);
    }
}