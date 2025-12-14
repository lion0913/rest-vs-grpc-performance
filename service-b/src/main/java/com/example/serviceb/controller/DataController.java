package com.example.serviceb.controller;

import com.example.serviceb.model.BatchDataRequest;
import com.example.serviceb.model.BatchDataResponse;
import com.example.serviceb.model.DataItem;
import com.example.serviceb.model.DataResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/data")
public class DataController {

    @PostMapping("/send")
    public DataResponse sendData(@RequestBody DataItem data) {
        long processedAt = System.currentTimeMillis();

        // 데이터 처리 시뮬레이션 (실제로는 아무것도 안함)
        log.debug("Received data via HTTP: {}", data.getId());

        return new DataResponse(true, "Data received successfully", processedAt);
    }

    @PostMapping("/batch")
    public BatchDataResponse sendBatchData(@RequestBody BatchDataRequest request) {
        long startTime = System.currentTimeMillis();

        int count = request.getItems() != null ? request.getItems().size() : 0;
        log.info("Received {} items via HTTP batch", count);

        long endTime = System.currentTimeMillis();

        return new BatchDataResponse(
            true,
            count,
            "Batch data received successfully",
            startTime,
            endTime
        );
    }
}