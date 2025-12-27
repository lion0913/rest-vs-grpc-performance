package com.example.servicea.client;

import com.example.proto.BatchDataGenerateRequest;
import com.example.proto.BatchDataResponse;
import com.example.proto.DataServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GrpcDataClient {

    @GrpcClient("service-b")
    private DataServiceGrpc.DataServiceBlockingStub dataServiceStub;

    public BatchDataResponse getBatchData(int count) {
        BatchDataGenerateRequest request = BatchDataGenerateRequest.newBuilder()
            .setCount(count)
            .build();
        return dataServiceStub.getBatchData(request);
    }
}