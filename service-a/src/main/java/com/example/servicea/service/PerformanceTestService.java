package com.example.servicea.service;

import com.example.proto.BatchDataRequest;
import com.example.proto.DataItem;
import com.example.proto.DataRequest;
import com.example.servicea.client.GrpcDataClient;
import com.example.servicea.client.HttpDataClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceTestService {

    private final HttpDataClient httpDataClient;
    private final GrpcDataClient grpcDataClient;
    private final DataGenerator dataGenerator;

    /**
     * HTTP 방식으로 배치 데이터 전송 테스트
     */
    public TestResult testHttpBatch(int totalCount, int batchSize) {
        log.info("Starting HTTP batch test: {} items, batch size: {}", totalCount, batchSize);

        long startTime = System.currentTimeMillis();
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < totalCount; i += batchSize) {
            int currentBatchSize = Math.min(batchSize, totalCount - i);
            List<com.example.servicea.model.DataItem> items = dataGenerator.generateDataItems(currentBatchSize);

            com.example.servicea.model.BatchDataRequest request =
                new com.example.servicea.model.BatchDataRequest(items);

            try {
                httpDataClient.sendBatchData(request).block();
                successCount += currentBatchSize;
            } catch (Exception e) {
                log.error("HTTP batch failed", e);
                failCount += currentBatchSize;
            }

            if ((i + currentBatchSize) % 10000 == 0) {
                log.info("HTTP Progress: {}/{}", i + currentBatchSize, totalCount);
            }
        }

        long endTime = System.currentTimeMillis();
        long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        TestResult result = new TestResult(
            "HTTP",
            totalCount,
            successCount,
            failCount,
            endTime - startTime,
            endMemory - startMemory
        );

        log.info("HTTP Test Result: {}", result);
        return result;
    }

    /**
     * gRPC 방식으로 배치 데이터 전송 테스트
     */
    public TestResult testGrpcBatch(int totalCount, int batchSize) {
        log.info("Starting gRPC batch test: {} items, batch size: {}", totalCount, batchSize);

        long startTime = System.currentTimeMillis();
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < totalCount; i += batchSize) {
            int currentBatchSize = Math.min(batchSize, totalCount - i);
            List<com.example.servicea.model.DataItem> modelItems = dataGenerator.generateDataItems(currentBatchSize);

            // Convert to Proto DataItem
            List<DataItem> protoItems = new ArrayList<>();
            for (com.example.servicea.model.DataItem item : modelItems) {
                protoItems.add(convertToProtoDataItem(item));
            }

            BatchDataRequest request = BatchDataRequest.newBuilder()
                .addAllItems(protoItems)
                .build();

            try {
                grpcDataClient.sendBatchData(request);
                successCount += currentBatchSize;
            } catch (Exception e) {
                log.error("gRPC batch failed", e);
                failCount += currentBatchSize;
            }

            if ((i + currentBatchSize) % 10000 == 0) {
                log.info("gRPC Progress: {}/{}", i + currentBatchSize, totalCount);
            }
        }

        long endTime = System.currentTimeMillis();
        long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        TestResult result = new TestResult(
            "gRPC",
            totalCount,
            successCount,
            failCount,
            endTime - startTime,
            endMemory - startMemory
        );

        log.info("gRPC Test Result: {}", result);
        return result;
    }

    /**
     * 두 프로토콜을 비교하고 결과를 response.md 파일에 저장
     */
    public void compareAndSaveResults(int totalCount, int batchSize) {
        log.info("Starting performance comparison test");

        // GC 실행
        System.gc();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        TestResult httpResult = testHttpBatch(totalCount, batchSize);

        // 테스트 간 대기
        log.info("Waiting between tests...");
        System.gc();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        TestResult grpcResult = testGrpcBatch(totalCount, batchSize);

        // 결과를 파일로 저장
        saveResultsToFile(httpResult, grpcResult, totalCount, batchSize);
    }

    /**
     * Model DataItem을 Proto DataItem으로 변환
     */
    private DataItem convertToProtoDataItem(com.example.servicea.model.DataItem item) {
        return DataItem.newBuilder()
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

    /**
     * 결과를 response.md 파일에 저장
     */
    private void saveResultsToFile(TestResult httpResult, TestResult grpcResult, int totalCount, int batchSize) {
        try (FileWriter writer = new FileWriter("../docs/response.md")) {
            writer.write("# HTTP vs gRPC 성능 비교 결과\n\n");
            writer.write("## 테스트 설정\n\n");
            writer.write(String.format("- 총 데이터 건수: %,d건\n", totalCount));
            writer.write(String.format("- 배치 크기: %,d건\n", batchSize));
            writer.write(String.format("- 데이터 크기: 약 2KB/건\n"));
            writer.write(String.format("- 총 데이터 크기: 약 %.2f MB\n\n", (totalCount * 2.0) / 1024));

            writer.write("## 테스트 결과\n\n");
            writer.write("### HTTP 결과\n\n");
            writer.write(String.format("- 소요 시간: %,d ms (%.2f초)\n", httpResult.durationMs, httpResult.durationMs / 1000.0));
            writer.write(String.format("- 처리량: %.2f 건/초\n", httpResult.getThroughput()));
            writer.write(String.format("- 성공: %,d건\n", httpResult.successCount));
            writer.write(String.format("- 실패: %,d건\n", httpResult.failCount));
            writer.write(String.format("- 메모리 사용량: %.2f MB\n\n", httpResult.memoryUsedBytes / (1024.0 * 1024.0)));

            writer.write("### gRPC 결과\n\n");
            writer.write(String.format("- 소요 시간: %,d ms (%.2f초)\n", grpcResult.durationMs, grpcResult.durationMs / 1000.0));
            writer.write(String.format("- 처리량: %.2f 건/초\n", grpcResult.getThroughput()));
            writer.write(String.format("- 성공: %,d건\n", grpcResult.successCount));
            writer.write(String.format("- 실패: %,d건\n", grpcResult.failCount));
            writer.write(String.format("- 메모리 사용량: %.2f MB\n\n", grpcResult.memoryUsedBytes / (1024.0 * 1024.0)));

            writer.write("## 비교 분석\n\n");

            double speedRatio = (double) httpResult.durationMs / grpcResult.durationMs;
            String fasterProtocol = speedRatio > 1 ? "gRPC" : "HTTP";
            double speedImprovement = Math.abs(speedRatio - 1) * 100;

            writer.write(String.format("### 속도 비교\n\n"));
            writer.write(String.format("- %s가 약 %.2f%% 더 빠름\n", fasterProtocol, speedImprovement));
            writer.write(String.format("- HTTP 소요 시간: %.2f초\n", httpResult.durationMs / 1000.0));
            writer.write(String.format("- gRPC 소요 시간: %.2f초\n\n", grpcResult.durationMs / 1000.0));

            double memoryRatio = (double) httpResult.memoryUsedBytes / grpcResult.memoryUsedBytes;
            String lessMemoryProtocol = memoryRatio > 1 ? "gRPC" : "HTTP";
            double memoryImprovement = Math.abs(memoryRatio - 1) * 100;

            writer.write(String.format("### 메모리 사용량 비교\n\n"));
            writer.write(String.format("- %s가 약 %.2f%% 더 적은 메모리 사용\n", lessMemoryProtocol, memoryImprovement));
            writer.write(String.format("- HTTP 메모리: %.2f MB\n", httpResult.memoryUsedBytes / (1024.0 * 1024.0)));
            writer.write(String.format("- gRPC 메모리: %.2f MB\n\n", grpcResult.memoryUsedBytes / (1024.0 * 1024.0)));

            writer.write("## 결론\n\n");
            writer.write(String.format("40만 건의 데이터(약 %.2f MB) 전송 시:\n\n", (totalCount * 2.0) / 1024));
            writer.write(String.format("1. **속도**: %s가 %s보다 %.2f%% 빠름\n", fasterProtocol,
                fasterProtocol.equals("gRPC") ? "HTTP" : "gRPC", speedImprovement));
            writer.write(String.format("2. **메모리**: %s가 %s보다 %.2f%% 적은 메모리 사용\n", lessMemoryProtocol,
                lessMemoryProtocol.equals("gRPC") ? "HTTP" : "gRPC", memoryImprovement));
            writer.write(String.format("3. **처리량**: HTTP %.2f 건/초 vs gRPC %.2f 건/초\n\n",
                httpResult.getThroughput(), grpcResult.getThroughput()));

            writer.write("**참고사항**:\n");
            writer.write("- gRPC는 HTTP/2 기반으로 멀티플렉싱, 헤더 압축 등의 이점이 있습니다.\n");
            writer.write("- Protocol Buffers는 JSON보다 직렬화/역직렬화가 빠르고 크기가 작습니다.\n");
            writer.write("- 네트워크 환경, 데이터 크기, 배치 크기 등에 따라 결과가 달라질 수 있습니다.\n");

            log.info("Results saved to docs/response.md");
        } catch (IOException e) {
            log.error("Failed to save results to file", e);
        }
    }

    /**
     * 테스트 결과를 담는 클래스
     */
    public static class TestResult {
        public final String protocol;
        public final int totalCount;
        public final int successCount;
        public final int failCount;
        public final long durationMs;
        public final long memoryUsedBytes;

        public TestResult(String protocol, int totalCount, int successCount, int failCount,
                          long durationMs, long memoryUsedBytes) {
            this.protocol = protocol;
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failCount = failCount;
            this.durationMs = durationMs;
            this.memoryUsedBytes = memoryUsedBytes;
        }

        public double getThroughput() {
            return (double) successCount / (durationMs / 1000.0);
        }

        @Override
        public String toString() {
            return String.format("%s - Total: %d, Success: %d, Fail: %d, Duration: %dms, Memory: %.2fMB, Throughput: %.2f items/sec",
                protocol, totalCount, successCount, failCount, durationMs,
                memoryUsedBytes / (1024.0 * 1024.0), getThroughput());
        }
    }
}