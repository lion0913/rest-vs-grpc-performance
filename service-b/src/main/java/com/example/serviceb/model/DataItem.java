package com.example.serviceb.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataItem {
    private String id;
    private String name;
    private String description;
    private String category;
    private String content;
    private Long timestamp;
    private String metadata1;
    private String metadata2;
    private String metadata3;
    private String metadata4;
    private String metadata5;
    private String additionalInfo;
    private Double value;
    private Integer status;
    private String tags;
}