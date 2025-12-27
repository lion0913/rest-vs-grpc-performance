package com.example.serviceb.controller;

import com.example.serviceb.model.BatchDataResponse;
import com.example.serviceb.model.DataItem;
import com.example.serviceb.service.DataGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataController {

    private final DataGenerator dataGenerator;

    /**
     * Service B가 데이터를 생성해서 반환
     */
    @GetMapping("/generate")
    public BatchDataResponse generateBatchData(@RequestParam int count) {
        long startTime = System.currentTimeMillis();

        log.info("Generating {} items via HTTP", count);
        List<DataItem> items = dataGenerator.generateDataItems(count);

        long endTime = System.currentTimeMillis();

        return new BatchDataResponse(
            true,
            items.size(),
            "Batch data generated successfully",
            startTime,
            endTime,
            items
        );
    }
}