package com.example.servicea.controller;

import com.example.servicea.service.PerformanceTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class PerformanceTestController {

    private final PerformanceTestService performanceTestService;

    /**
     * HTTP 성능 테스트
     */
    @PostMapping("/http")
    public Map<String, Object> testHttp(
            @RequestParam(defaultValue = "400000") int totalCount,
            @RequestParam(defaultValue = "1000") int batchSize) {

        log.info("Starting HTTP performance test: totalCount={}, batchSize={}", totalCount, batchSize);

        PerformanceTestService.TestResult result = performanceTestService.testHttpBatch(totalCount, batchSize);

        Map<String, Object> response = new HashMap<>();
        response.put("protocol", result.protocol);
        response.put("totalCount", result.totalCount);
        response.put("successCount", result.successCount);
        response.put("failCount", result.failCount);
        response.put("durationMs", result.durationMs);
        response.put("durationSec", result.durationMs / 1000.0);
        response.put("memoryUsedMB", result.memoryUsedBytes / (1024.0 * 1024.0));
        response.put("throughput", result.getThroughput());

        return response;
    }

    /**
     * gRPC 성능 테스트
     */
    @PostMapping("/grpc")
    public Map<String, Object> testGrpc(
            @RequestParam(defaultValue = "400000") int totalCount,
            @RequestParam(defaultValue = "1000") int batchSize) {

        log.info("Starting gRPC performance test: totalCount={}, batchSize={}", totalCount, batchSize);

        PerformanceTestService.TestResult result = performanceTestService.testGrpcBatch(totalCount, batchSize);

        Map<String, Object> response = new HashMap<>();
        response.put("protocol", result.protocol);
        response.put("totalCount", result.totalCount);
        response.put("successCount", result.successCount);
        response.put("failCount", result.failCount);
        response.put("durationMs", result.durationMs);
        response.put("durationSec", result.durationMs / 1000.0);
        response.put("memoryUsedMB", result.memoryUsedBytes / (1024.0 * 1024.0));
        response.put("throughput", result.getThroughput());

        return response;
    }

    /**
     * HTTP vs gRPC 비교 테스트 실행 및 결과를 response.md에 저장
     */
    @PostMapping("/compare")
    public Map<String, String> compareProtocols(
            @RequestParam(defaultValue = "400000") int totalCount,
            @RequestParam(defaultValue = "1000") int batchSize) {

        log.info("Starting comparison test: totalCount={}, batchSize={}", totalCount, batchSize);

        performanceTestService.compareAndSaveResults(totalCount, batchSize);

        Map<String, String> response = new HashMap<>();
        response.put("status", "completed");
        response.put("message", "Performance comparison completed. Results saved to docs/response.md");
        response.put("totalCount", String.valueOf(totalCount));
        response.put("batchSize", String.valueOf(batchSize));

        return response;
    }
}