package com.example.serviceb.service;

import com.example.serviceb.model.ServerPerformanceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PerformanceMetricsService {

    // 프로토콜별로 측정 결과를 저장
    private final Map<String, List<ServerPerformanceMetrics>> metricsStore = new ConcurrentHashMap<>();

    public void recordMetrics(ServerPerformanceMetrics metrics) {
        log.info("Recording server metrics: protocol={}, duration={}ms, memory={}MB",
            metrics.getProtocol(), metrics.getDurationMs(), metrics.getMemoryUsedMB());

        metricsStore.computeIfAbsent(metrics.getProtocol(), k -> new ArrayList<>()).add(metrics);
    }

    public List<ServerPerformanceMetrics> getMetrics(String protocol) {
        return new ArrayList<>(metricsStore.getOrDefault(protocol, new ArrayList<>()));
    }

    public Map<String, List<ServerPerformanceMetrics>> getAllMetrics() {
        return new ConcurrentHashMap<>(metricsStore);
    }

    public void clearMetrics() {
        log.info("Clearing all server metrics");
        metricsStore.clear();
    }

    public void clearMetrics(String protocol) {
        log.info("Clearing server metrics for protocol: {}", protocol);
        metricsStore.remove(protocol);
    }
}