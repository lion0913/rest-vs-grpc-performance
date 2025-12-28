package com.example.serviceb.controller;

import com.example.serviceb.model.BatchDataResponse;
import com.example.serviceb.model.DataItem;
import com.example.serviceb.model.ServerPerformanceMetrics;
import com.example.serviceb.service.DataGenerator;
import com.example.serviceb.service.PerformanceMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataController {

    private final DataGenerator dataGenerator;
    private final PerformanceMetricsService metricsService;

    /**
     * Service B가 데이터를 생성해서 반환
     */
    @GetMapping("/generate")
    public BatchDataResponse generateBatchData(@RequestParam int count) {
        long startTime = System.currentTimeMillis();

        // MemoryMXBean을 사용한 정확한 힙 메모리 측정
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long startMemory = memoryBean.getHeapMemoryUsage().getUsed();

        log.info("Generating {} items via HTTP", count);

        // 데이터 생성 시작
        long dataGenStart = System.currentTimeMillis();
        List<DataItem> items = dataGenerator.generateDataItems(count);
        long dataGenEnd = System.currentTimeMillis();

        // 직렬화 시작 (Spring이 자동으로 JSON 변환)
        long serializationStart = System.currentTimeMillis();
        BatchDataResponse response = new BatchDataResponse(
            true,
            items.size(),
            "Batch data generated successfully",
            startTime,
            System.currentTimeMillis(),
            items
        );
        long serializationEnd = System.currentTimeMillis();

        long endTime = System.currentTimeMillis();
        long endMemory = memoryBean.getHeapMemoryUsage().getUsed();

        // 메모리 증가량 계산
        long memoryIncrease = endMemory - startMemory;

        // 서버 측 성능 측정 결과 저장
        ServerPerformanceMetrics metrics = new ServerPerformanceMetrics(
            "HTTP",
            count,
            startTime,
            endTime,
            endTime - startTime,
            memoryIncrease,
            dataGenEnd - dataGenStart,
            serializationEnd - serializationStart
        );
        metricsService.recordMetrics(metrics);

        log.info("HTTP Server metrics - Duration: {}ms, Memory: {:.2f}MB, DataGen: {}ms, Serialization: {}ms",
            metrics.getDurationMs(), metrics.getMemoryUsedMB(),
            metrics.getDataGenerationMs(), metrics.getSerializationMs());

        return response;
    }

    /**
     * 서버 측 성능 측정 결과 조회
     */
    @GetMapping("/metrics")
    public Map<String, List<ServerPerformanceMetrics>> getMetrics() {
        return metricsService.getAllMetrics();
    }

    /**
     * 서버 측 성능 측정 결과 초기화
     */
    @DeleteMapping("/metrics")
    public Map<String, String> clearMetrics() {
        metricsService.clearMetrics();
        return Map.of("status", "success", "message", "All server metrics cleared");
    }
}