package com.example.serviceb.service;

import com.example.proto.*;
import com.example.serviceb.model.DataItem;
import com.example.serviceb.model.ServerPerformanceMetrics;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class GrpcDataService extends DataServiceGrpc.DataServiceImplBase {

    private final DataGenerator dataGenerator;
    private final PerformanceMetricsService metricsService;

    /**
     * Service B가 데이터를 생성해서 반환
     */
    @Override
    public void getBatchData(BatchDataGenerateRequest request, StreamObserver<BatchDataResponse> responseObserver) {
        long startTime = System.currentTimeMillis();

        // MemoryMXBean을 사용한 정확한 힙 메모리 측정
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long startMemory = memoryBean.getHeapMemoryUsage().getUsed();

        int count = request.getCount();
        log.info("Generating {} items via gRPC", count);

        // 데이터 생성 시작
        long dataGenStart = System.currentTimeMillis();
        List<DataItem> items = dataGenerator.generateDataItems(count);
        long dataGenEnd = System.currentTimeMillis();

        // 직렬화 시작 (Proto 변환)
        long serializationStart = System.currentTimeMillis();
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
        long serializationEnd = System.currentTimeMillis();

        long endTime = System.currentTimeMillis();
        long endMemory = memoryBean.getHeapMemoryUsage().getUsed();

        // 메모리 증가량 계산
        long memoryIncrease = endMemory - startMemory;

        // 서버 측 성능 측정 결과 저장
        ServerPerformanceMetrics metrics = new ServerPerformanceMetrics(
            "gRPC",
            count,
            startTime,
            endTime,
            endTime - startTime,
            memoryIncrease,
            dataGenEnd - dataGenStart,
            serializationEnd - serializationStart
        );
        metricsService.recordMetrics(metrics);

        log.info("gRPC Server metrics - Duration: {}ms, Memory: {:.2f}MB, DataGen: {}ms, Serialization: {}ms",
            metrics.getDurationMs(), metrics.getMemoryUsedMB(),
            metrics.getDataGenerationMs(), metrics.getSerializationMs());

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