package com.example.servicea.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerPerformanceMetrics {
    private String protocol;
    private int count;
    private long startTime;
    private long endTime;
    private long durationMs;
    private long memoryUsedBytes;
    private long dataGenerationMs;
    private long serializationMs;

    public double getThroughput() {
        return (double) count / (durationMs / 1000.0);
    }

    public double getMemoryUsedMB() {
        return memoryUsedBytes / (1024.0 * 1024.0);
    }
}