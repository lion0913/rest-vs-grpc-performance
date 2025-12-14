package com.example.servicea.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchDataResponse {
    private boolean success;
    private int processedCount;
    private String message;
    private Long startTime;
    private Long endTime;
}