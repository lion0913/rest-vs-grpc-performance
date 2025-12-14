package com.example.servicea.client;

import com.example.servicea.model.BatchDataRequest;
import com.example.servicea.model.BatchDataResponse;
import com.example.servicea.model.DataItem;
import com.example.servicea.model.DataResponse;
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

    public Mono<DataResponse> sendData(DataItem data) {
        return webClient.post()
            .uri("/api/data/send")
            .bodyValue(data)
            .retrieve()
            .bodyToMono(DataResponse.class);
    }

    public Mono<BatchDataResponse> sendBatchData(BatchDataRequest request) {
        return webClient.post()
            .uri("/api/data/batch")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(BatchDataResponse.class);
    }
}