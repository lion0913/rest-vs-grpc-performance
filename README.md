# HTTP vs gRPC 성능 비교 프로젝트

이 프로젝트는 MSA 환경에서 HTTP(REST)와 gRPC 통신의 성능을 비교하기 위한 스프링부트 애플리케이션입니다.

## 프로젝트 구조

```
response-test/
├── service-a/          # 클라이언트 서비스
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/example/servicea/
│   │       │       ├── client/           # HTTP & gRPC 클라이언트
│   │       │       ├── controller/       # 테스트 API 컨트롤러
│   │       │       ├── service/          # 성능 측정 서비스
│   │       │       └── model/            # 데이터 모델
│   │       ├── proto/                    # gRPC Proto 파일
│   │       └── resources/
│   └── build.gradle
│
├── service-b/          # 서버 서비스
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/example/serviceb/
│   │       │       ├── controller/       # HTTP REST API
│   │       │       ├── service/          # gRPC 서비스
│   │       │       └── model/            # 데이터 모델
│   │       ├── proto/                    # gRPC Proto 파일
│   │       └── resources/
│   └── build.gradle
│
├── shared/
│   └── proto/          # 공유 Proto 파일
│
└── docs/
    ├── request.md      # 요구사항
    └── response.md     # 테스트 결과 (자동 생성)
```

## 기술 스택

- **Java**: 17
- **Spring Boot**: 3.2.0
- **gRPC**: 1.60.0
- **Protocol Buffers**: 3.25.1
- **Gradle**: 빌드 도구
- **Lombok**: 코드 간소화
- **Spring WebFlux**: 비동기 HTTP 클라이언트

## 테스트 시나리오

- **데이터 건수**: 400,000건 (기본값)
- **데이터 크기**: 약 2KB/건
- **총 데이터 크기**: 약 781 MB
- **배치 크기**: 1,000건 (기본값)
- **통신 방식**: HTTP(REST) vs gRPC

## 빌드 방법

### 사전 요구사항

- Java 17 이상
- Gradle (또는 프로젝트 내 gradlew 사용)

### 빌드

각 서비스를 개별적으로 빌드합니다:

```bash
# Service B 빌드
cd service-b
./gradlew clean build

# Service A 빌드
cd ../service-a
./gradlew clean build
```

또는 한번에 빌드:

```bash
# Service B
cd service-b && ./gradlew clean build && cd ..

# Service A
cd service-a && ./gradlew clean build && cd ..
```

## 빠른 시작 (자동화 스크립트)

프로젝트 루트에서 제공하는 스크립트를 사용하여 간편하게 실행할 수 있습니다.

### 1. 서비스 시작

```bash
./start-services.sh
```

이 스크립트는 다음을 수행합니다:
- Gradle wrapper 생성 (필요한 경우)
- 두 서비스 빌드
- Service B와 Service A를 백그라운드에서 실행
- 로그를 `service-a.log`와 `service-b.log`에 저장

### 2. 성능 테스트 실행

```bash
# 기본 설정 (400,000건, 배치 크기 1,000)
./run-test.sh

# 커스텀 설정
./run-test.sh 100000 500  # 100,000건, 배치 크기 500
```

### 3. 서비스 종료

```bash
./stop-services.sh
```

## Docker로 실제 MSA 환경 시뮬레이션

로컬 환경에서는 네트워크 지연이 없어 gRPC의 장점이 충분히 드러나지 않습니다. Docker 컨테이너로 각 서비스를 분리하면 더 현실적인 테스트가 가능합니다.

### 사전 요구사항

- Docker Desktop 설치
- Docker Compose

### 1. 기본 Docker 테스트 (컨테이너 간 통신)

```bash
./docker-test.sh --basic
```

각 서비스가 별도 컨테이너에서 실행되어 실제 네트워크 통신이 발생합니다.

### 2. 네트워크 지연 시뮬레이션 테스트

```bash
./docker-test.sh --latency
```

Toxiproxy를 사용하여 10ms ± 5ms의 네트워크 지연을 추가합니다. 실제 MSA 환경(쿠버네티스 파드 간 통신)과 유사한 조건을 만듭니다.

**Toxiproxy 설정:**
- HTTP: 10ms ± 5ms 지연
- gRPC: 10ms ± 5ms 지연
- 실제 인터넷 지연에 비해 작지만, 같은 데이터센터 내 파드 간 통신을 시뮬레이션

### 3. 컨테이너 중지

```bash
./docker-test.sh --stop
```

### 4. 완전 삭제 (이미지 포함)

```bash
./docker-test.sh --clean
```

### Docker 테스트의 장점

1. **실제 네트워크 통신**: localhost가 아닌 컨테이너 간 통신
2. **격리된 환경**: 각 서비스가 독립된 컨테이너에서 실행
3. **네트워크 지연 시뮬레이션**: Toxiproxy로 실제 환경 재현
4. **MSA 환경 근사**: 쿠버네티스 파드 간 통신과 유사

### 예상 결과 차이

| 환경 | HTTP vs gRPC 속도 | gRPC 장점 |
|------|------------------|----------|
| **로컬 (localhost)** | 거의 동일 | 메모리만 |
| **Docker (기본)** | gRPC 10-20% 빠름 | 직렬화 + 프로토콜 |
| **Docker (지연)** | gRPC 20-40% 빠름 | 헤더압축 + HTTP/2 |
| **실제 MSA** | gRPC 30-50% 빠름 | 모든 최적화 |

### 수동 Docker 실행

```bash
# 기본 테스트
docker-compose up -d --build
curl -X POST "http://localhost:8080/api/test/compare?totalCount=400000&batchSize=1000"
docker-compose down

# 네트워크 지연 테스트
docker-compose -f docker-compose-with-latency.yml up -d --build
curl -X POST "http://localhost:8080/api/test/compare?totalCount=400000&batchSize=1000"
docker-compose -f docker-compose-with-latency.yml down
```

### Toxiproxy 설정 변경

네트워크 지연을 조정하려면 `toxiproxy.json` 파일을 수정하세요:

```json
{
  "attributes": {
    "latency": 50,    // 평균 지연 (ms)
    "jitter": 20      // 지연 변동폭 (ms)
  }
}
```

일반적인 네트워크 지연 값:
- **같은 AZ (Availability Zone)**: 1-2ms
- **같은 리전, 다른 AZ**: 5-10ms
- **다른 리전 (국내)**: 20-50ms
- **글로벌 (대륙 간)**: 100-300ms

## 실행 방법 (수동)

### 1. Service B (서버) 실행

먼저 서버 역할을 하는 Service B를 실행합니다:

```bash
cd service-b
./gradlew bootRun
```

또는 빌드된 JAR 실행:

```bash
java -jar build/libs/service-b-1.0.0.jar
```

**포트 정보:**
- HTTP API: `http://localhost:8081`
- gRPC: `localhost:9091`
- Actuator: `http://localhost:8081/actuator`

### 2. Service A (클라이언트) 실행

새 터미널에서 클라이언트 역할을 하는 Service A를 실행합니다:

```bash
cd service-a
./gradlew bootRun
```

또는 빌드된 JAR 실행:

```bash
java -jar build/libs/service-a-1.0.0.jar
```

**포트 정보:**
- HTTP API: `http://localhost:8080`
- Actuator: `http://localhost:8080/actuator`

## 성능 테스트 실행

Service A와 Service B가 모두 실행된 상태에서 테스트를 수행합니다.

### 1. HTTP vs gRPC 전체 비교 테스트

```bash
curl -X POST "http://localhost:8080/api/test/compare?totalCount=400000&batchSize=1000"
```

이 명령은 다음을 수행합니다:
1. HTTP로 400,000건 전송 및 성능 측정
2. gRPC로 400,000건 전송 및 성능 측정
3. 결과 비교 및 `docs/response.md` 파일에 저장

### 2. HTTP만 테스트

```bash
curl -X POST "http://localhost:8080/api/test/http?totalCount=400000&batchSize=1000"
```

### 3. gRPC만 테스트

```bash
curl -X POST "http://localhost:8080/api/test/grpc?totalCount=400000&batchSize=1000"
```

### 4. 파라미터 커스터마이징

```bash
# 10만건, 배치 크기 500으로 테스트
curl -X POST "http://localhost:8080/api/test/compare?totalCount=100000&batchSize=500"

# 100만건, 배치 크기 2000으로 테스트
curl -X POST "http://localhost:8080/api/test/compare?totalCount=1000000&batchSize=2000"
```

## 테스트 결과 확인

테스트가 완료되면 `docs/response.md` 파일에 다음 정보가 자동으로 저장됩니다:

- 테스트 설정 정보
- HTTP 성능 결과
- gRPC 성능 결과
- 두 프로토콜 비교 분석
- 결론 및 권장사항

```bash
# 결과 확인
cat docs/response.md
```

## 모니터링

각 서비스는 Spring Boot Actuator를 통해 모니터링 가능합니다:

### Service A
```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

### Service B
```bash
# Health check
curl http://localhost:8081/actuator/health

# Metrics
curl http://localhost:8081/actuator/metrics

# Prometheus metrics
curl http://localhost:8081/actuator/prometheus
```

## 측정 지표

프로젝트는 다음 지표들을 측정합니다:

1. **전송 속도 (Duration)**
   - 전체 데이터 전송에 걸린 시간 (밀리초, 초)

2. **처리량 (Throughput)**
   - 초당 처리 가능한 데이터 건수

3. **메모리 사용량 (Memory Usage)**
   - 테스트 전후 메모리 사용량 차이

4. **성공/실패 건수**
   - 정상 처리된 건수와 실패한 건수

## 주의사항

1. **힙 메모리 설정**: 대량의 데이터를 처리하므로 충분한 힙 메모리가 필요할 수 있습니다.

   ```bash
   # 힙 메모리 증가하여 실행 (예: 2GB)
   java -Xmx2g -jar service-a/build/libs/service-a-1.0.0.jar
   java -Xmx2g -jar service-b/build/libs/service-b-1.0.0.jar
   ```

2. **네트워크**: 로컬 환경에서 테스트하므로 실제 네트워크 지연은 반영되지 않습니다.

3. **배치 크기**: 배치 크기가 너무 크면 메모리 부족이 발생할 수 있습니다.

4. **GC 영향**: 메모리 측정 시 GC의 영향을 받을 수 있습니다.

## 트러블슈팅

### gRPC 연결 실패

```
io.grpc.StatusRuntimeException: UNAVAILABLE: io exception
```

**해결방법**: Service B가 먼저 실행되었는지 확인하고, gRPC 포트 9091이 열려있는지 확인합니다.

### HTTP 연결 실패

```
java.net.ConnectException: Connection refused
```

**해결방법**: Service B의 HTTP 서버(8081)가 실행 중인지 확인합니다.

### Out of Memory 오류

```
java.lang.OutOfMemoryError: Java heap space
```

**해결방법**: 힙 메모리를 증가시키거나 배치 크기를 줄입니다.

```bash
# 힙 메모리 증가
java -Xmx4g -jar service-a/build/libs/service-a-1.0.0.jar

# 또는 배치 크기 감소
curl -X POST "http://localhost:8080/api/test/compare?totalCount=400000&batchSize=500"
```

## 개발자 정보

- **목적**: HTTP와 gRPC 성능 비교 블로그 포스팅
- **환경**: MSA 마이크로서비스 간 통신 시뮬레이션
- **데이터**: 40만건, 약 2KB/건

## 라이선스

이 프로젝트는 학습 및 연구 목적으로 작성되었습니다.