package com.example.serviceb.service;

import com.example.proto.*;
import com.example.serviceb.model.DataItem;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class GrpcDataService extends DataServiceGrpc.DataServiceImplBase {

    private final DataGenerator dataGenerator;

    /**
     * Service B가 데이터를 생성해서 반환
     */
    @Override
    public void getBatchData(BatchDataGenerateRequest request, StreamObserver<BatchDataResponse> responseObserver) {
        long startTime = System.currentTimeMillis();

        int count = request.getCount();
        log.info("Generating {} items via gRPC", count);

        List<DataItem> items = dataGenerator.generateDataItems(count);

        BatchDataResponse.Builder responseBuilder = BatchDataResponse.newBuilder()
            .setSuccess(true)
            .setProcessedCount(items.size())
            .setMessage("Batch data generated successfully")
            .setStartTime(startTime)
            .setEndTime(System.currentTimeMillis());

        // Java DataItem을 Proto DataItem으로 변환
        for (DataItem item : items) {
            com.example.proto.DataItem protoItem = convertToProto(item);
            responseBuilder.addItems(protoItem);
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    private com.example.proto.DataItem convertToProto(DataItem item) {
        return com.example.proto.DataItem.newBuilder()
            .setId(item.getId())
            .setName(item.getName())
            .setDescription(item.getDescription())
            .setCategory(item.getCategory())
            .setContent(item.getContent())
            .setTimestamp(item.getTimestamp())
            .setMetadata1(item.getMetadata1())
            .setMetadata2(item.getMetadata2())
            .setMetadata3(item.getMetadata3())
            .setMetadata4(item.getMetadata4())
            .setMetadata5(item.getMetadata5())
            .setAdditionalInfo(item.getAdditionalInfo())
            .setValue(item.getValue())
            .setStatus(item.getStatus())
            .setTags(item.getTags())
            .build();
    }
}