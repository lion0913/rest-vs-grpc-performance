package com.example.servicea.client;

import com.example.proto.*;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GrpcDataClient {

    @GrpcClient("service-b")
    private DataServiceGrpc.DataServiceBlockingStub dataServiceStub;

    public DataResponse sendData(DataRequest request) {
        return dataServiceStub.sendData(request);
    }

    public BatchDataResponse sendBatchData(BatchDataRequest request) {
        return dataServiceStub.sendBatchData(request);
    }
}