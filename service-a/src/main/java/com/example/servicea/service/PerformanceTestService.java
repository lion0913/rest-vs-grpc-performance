package com.example.servicea.service;

import com.example.servicea.client.GrpcDataClient;
import com.example.servicea.client.HttpDataClient;
import com.example.servicea.model.BatchDataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    /**
     * HTTP 방식으로 배치 데이터 수신 테스트 (Service B가 데이터 생성)
     */
    public TestResult testHttpBatch(int totalCount, int batchSize) {
        log.info("Starting HTTP batch test: {} items, batch size: {}", totalCount, batchSize);

        long startTime = System.currentTimeMillis();
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < totalCount; i += batchSize) {
            int currentBatchSize = Math.min(batchSize, totalCount - i);

            try {
                BatchDataResponse response =
                    httpDataClient.getBatchData(currentBatchSize).block();

                if (response != null && response.isSuccess()) {
                    successCount += currentBatchSize;
                    log.debug("Received {} items from Service B via HTTP", response.getProcessedCount());
                }
            } catch (Exception e) {
                log.error("HTTP batch receive failed", e);
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
     * gRPC 방식으로 배치 데이터 수신 테스트 (Service B가 데이터 생성)
     */
    public TestResult testGrpcBatch(int totalCount, int batchSize) {
        log.info("Starting gRPC batch test: {} items, batch size: {}", totalCount, batchSize);

        long startTime = System.currentTimeMillis();
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < totalCount; i += batchSize) {
            int currentBatchSize = Math.min(batchSize, totalCount - i);

            try {
                com.example.proto.BatchDataResponse response =
                    grpcDataClient.getBatchData(currentBatchSize);

                if (response != null && response.getSuccess()) {
                    successCount += currentBatchSize;
                    log.debug("Received {} items from Service B via gRPC", response.getProcessedCount());
                }
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
     * 두 프로토콜을 비교하고 결과를 response-basic-multiple.md 파일에 저장
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
     * 10회 반복 테스트를 수행하고 평균 결과를 저장
     */
    public void compareAndSaveResultsWithMultipleRuns(int totalCount, int batchSize, int runs, int intervalSeconds, boolean withLatency) {
        log.info("Starting {} runs of performance comparison test with {} seconds interval (latency: {})",
            runs, intervalSeconds, withLatency ? "enabled" : "disabled");

        List<TestResult> httpResults = new ArrayList<>();
        List<TestResult> grpcResults = new ArrayList<>();

        for (int run = 1; run <= runs; run++) {
            log.info("===== Run {}/{} =====", run, runs);

            // GC 실행
            System.gc();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            TestResult httpResult = testHttpBatch(totalCount, batchSize);
            httpResults.add(httpResult);

            // 테스트 간 대기
            log.info("Waiting between HTTP and gRPC tests...");
            System.gc();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            TestResult grpcResult = testGrpcBatch(totalCount, batchSize);
            grpcResults.add(grpcResult);

            // 다음 회차 전 대기 (마지막 회차가 아닐 경우)
            if (run < runs) {
                log.info("Waiting {} seconds before next run...", intervalSeconds);
                try {
                    Thread.sleep(intervalSeconds * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // 결과를 파일로 저장
        saveMultipleRunsResultsToFile(httpResults, grpcResults, totalCount, batchSize, withLatency);
    }

    /**
     * 결과를 response-basic.md 파일에 저장
     */
    private void saveResultsToFile(TestResult httpResult, TestResult grpcResult, int totalCount, int batchSize) {
        // Docker 환경에서는 /docs, 로컬에서는 ../docs 사용
        String filePath = new java.io.File("/docs").exists() ? "/docs/response-basic.md" : "../docs/response-basic.md";
        try (FileWriter writer = new FileWriter(filePath)) {
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

            log.info("Results saved to docs/response-basic-multiple.md");
        } catch (IOException e) {
            log.error("Failed to save results to file", e);
        }
    }

    /**
     * 10회 반복 테스트 결과를 파일에 저장
     */
    private void saveMultipleRunsResultsToFile(List<TestResult> httpResults, List<TestResult> grpcResults, int totalCount, int batchSize, boolean withLatency) {
        // 파일명 결정: latency 여부에 따라 분기
        String fileName = withLatency ? "response-latency-multiple.md" : "response-basic-multiple.md";
        String filePath = new java.io.File("/docs").exists() ? "/docs/" + fileName : "../docs/" + fileName;

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("# HTTP vs gRPC 성능 비교 결과 (10회 평균)\n\n");
            writer.write("## 테스트 설정\n\n");
            writer.write(String.format("- 총 데이터 건수: %,d건\n", totalCount));
            writer.write(String.format("- 배치 크기: %,d건\n", batchSize));
            writer.write(String.format("- 데이터 크기: 약 2KB/건\n"));
            writer.write(String.format("- 총 데이터 크기: 약 %.2f MB\n", (totalCount * 2.0) / 1024));
            writer.write(String.format("- 테스트 반복 횟수: %d회\n", httpResults.size()));
            writer.write(String.format("- 네트워크 지연: %s\n\n", withLatency ? "활성화 (Toxiproxy 2ms ± 5ms)" : "비활성화"));

            // 회차별 결과 표
            writer.write("## 회차별 테스트 결과\n\n");
            writer.write("### HTTP (REST API)\n\n");
            writer.write("| 회차 | 소요 시간 (ms) | 처리량 (건/초) | 메모리 사용량 (MB) |\n");
            writer.write("|------|---------------|---------------|-------------------|\n");

            for (int i = 0; i < httpResults.size(); i++) {
                TestResult result = httpResults.get(i);
                writer.write(String.format("| %d회 | %,d | %.2f | %.2f |\n",
                    i + 1,
                    result.durationMs,
                    result.getThroughput(),
                    result.memoryUsedBytes / (1024.0 * 1024.0)));
            }
            writer.write("\n");

            writer.write("### gRPC (Protocol Buffers)\n\n");
            writer.write("| 회차 | 소요 시간 (ms) | 처리량 (건/초) | 메모리 사용량 (MB) |\n");
            writer.write("|------|---------------|---------------|-------------------|\n");

            for (int i = 0; i < grpcResults.size(); i++) {
                TestResult result = grpcResults.get(i);
                writer.write(String.format("| %d회 | %,d | %.2f | %.2f |\n",
                    i + 1,
                    result.durationMs,
                    result.getThroughput(),
                    result.memoryUsedBytes / (1024.0 * 1024.0)));
            }
            writer.write("\n");

            // 평균 계산
            double avgHttpDuration = httpResults.stream().mapToLong(r -> r.durationMs).average().orElse(0);
            double avgHttpThroughput = httpResults.stream().mapToDouble(r -> r.getThroughput()).average().orElse(0);
            double avgHttpMemory = httpResults.stream().mapToLong(r -> r.memoryUsedBytes).average().orElse(0) / (1024.0 * 1024.0);

            double avgGrpcDuration = grpcResults.stream().mapToLong(r -> r.durationMs).average().orElse(0);
            double avgGrpcThroughput = grpcResults.stream().mapToDouble(r -> r.getThroughput()).average().orElse(0);
            double avgGrpcMemory = grpcResults.stream().mapToLong(r -> r.memoryUsedBytes).average().orElse(0) / (1024.0 * 1024.0);

            // 평균 결과
            writer.write("## 평균 테스트 결과\n\n");
            writer.write("| 프로토콜 | 평균 소요 시간 (ms) | 평균 처리량 (건/초) | 평균 메모리 사용량 (MB) |\n");
            writer.write("|---------|-------------------|-------------------|----------------------|\n");
            writer.write(String.format("| HTTP | %.2f | %.2f | %.2f |\n", avgHttpDuration, avgHttpThroughput, avgHttpMemory));
            writer.write(String.format("| gRPC | %.2f | %.2f | %.2f |\n\n", avgGrpcDuration, avgGrpcThroughput, avgGrpcMemory));

            // 비교 분석
            writer.write("## 비교 분석\n\n");

            double speedRatio = avgHttpDuration / avgGrpcDuration;
            String fasterProtocol = speedRatio > 1 ? "gRPC" : "HTTP";
            double speedImprovement = Math.abs(speedRatio - 1) * 100;

            writer.write("### 속도 비교\n\n");
            writer.write(String.format("- **%s가 평균 %.2f%% 더 빠름**\n", fasterProtocol, speedImprovement));
            writer.write(String.format("- HTTP 평균 소요 시간: %.2f ms (%.2f초)\n", avgHttpDuration, avgHttpDuration / 1000.0));
            writer.write(String.format("- gRPC 평균 소요 시간: %.2f ms (%.2f초)\n\n", avgGrpcDuration, avgGrpcDuration / 1000.0));

            double throughputRatio = avgGrpcThroughput / avgHttpThroughput;
            double throughputImprovement = (throughputRatio - 1) * 100;

            writer.write("### 처리량 비교\n\n");
            if (throughputImprovement > 0) {
                writer.write(String.format("- **gRPC가 평균 %.2f%% 더 높은 처리량**\n", throughputImprovement));
            } else {
                writer.write(String.format("- **HTTP가 평균 %.2f%% 더 높은 처리량**\n", Math.abs(throughputImprovement)));
            }
            writer.write(String.format("- HTTP 평균 처리량: %.2f 건/초\n", avgHttpThroughput));
            writer.write(String.format("- gRPC 평균 처리량: %.2f 건/초\n\n", avgGrpcThroughput));

            double memoryRatio = avgHttpMemory / avgGrpcMemory;
            String lessMemoryProtocol = memoryRatio > 1 ? "gRPC" : "HTTP";
            double memoryImprovement = Math.abs(memoryRatio - 1) * 100;

            writer.write("### 메모리 사용량 비교\n\n");
            writer.write(String.format("- **%s가 평균 %.2f%% 더 적은 메모리 사용**\n", lessMemoryProtocol, memoryImprovement));
            writer.write(String.format("- HTTP 평균 메모리: %.2f MB\n", avgHttpMemory));
            writer.write(String.format("- gRPC 평균 메모리: %.2f MB\n\n", avgGrpcMemory));

            // 결론
            writer.write("## 결론\n\n");
            writer.write(String.format("40만 건의 데이터(약 %.2f MB) 전송 시 **10회 평균 결과**:\n\n", (totalCount * 2.0) / 1024));
            writer.write(String.format("1. **속도**: %s가 %s보다 평균 **%.2f%%** 빠름\n", fasterProtocol,
                fasterProtocol.equals("gRPC") ? "HTTP" : "gRPC", speedImprovement));
            writer.write(String.format("2. **처리량**: gRPC가 HTTP보다 평균 **%.2f%%** %s\n",
                Math.abs(throughputImprovement),
                throughputImprovement > 0 ? "높음" : "낮음"));
            writer.write(String.format("3. **메모리**: %s가 %s보다 평균 **%.2f%%** 적은 메모리 사용\n\n", lessMemoryProtocol,
                lessMemoryProtocol.equals("gRPC") ? "HTTP" : "gRPC", memoryImprovement));

            writer.write("**참고사항**:\n");
            writer.write("- gRPC는 HTTP/2 기반으로 멀티플렉싱, 헤더 압축 등의 이점이 있습니다.\n");
            writer.write("- Protocol Buffers는 JSON보다 직렬화/역직렬화가 빠르고 크기가 작습니다.\n");
            writer.write("- 네트워크 환경, 데이터 크기, 배치 크기 등에 따라 결과가 달라질 수 있습니다.\n");
            writer.write("- 10회 반복 테스트를 통해 환경 변수를 최소화하고 평균적인 성능을 측정했습니다.\n");

            log.info("Multiple runs results saved to " + filePath);
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