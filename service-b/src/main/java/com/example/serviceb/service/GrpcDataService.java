package com.example.serviceb.service;

import com.example.proto.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
public class GrpcDataService extends DataServiceGrpc.DataServiceImplBase {

    @Override
    public void sendData(DataRequest request, StreamObserver<DataResponse> responseObserver) {
        long processedAt = System.currentTimeMillis();

        log.debug("Received data via gRPC: {}", request.getData().getId());

        DataResponse response = DataResponse.newBuilder()
            .setSuccess(true)
            .setMessage("Data received successfully")
            .setProcessedAt(processedAt)
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void sendBatchData(BatchDataRequest request, StreamObserver<BatchDataResponse> responseObserver) {
        long startTime = System.currentTimeMillis();

        int count = request.getItemsCount();
        log.info("Received {} items via gRPC batch", count);

        long endTime = System.currentTimeMillis();

        BatchDataResponse response = BatchDataResponse.newBuilder()
            .setSuccess(true)
            .setProcessedCount(count)
            .setMessage("Batch data received successfully")
            .setStartTime(startTime)
            .setEndTime(endTime)
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}