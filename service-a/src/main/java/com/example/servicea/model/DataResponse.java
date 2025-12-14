package com.example.servicea.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataResponse {
    private boolean success;
    private String message;
    private Long processedAt;
}