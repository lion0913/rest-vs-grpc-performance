#!/bin/bash

echo "======================================"
echo "HTTP vs gRPC Performance Test (Docker)"
echo "======================================"
echo ""

# 사용법 출력
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --basic       기본 테스트 (네트워크 지연 없음)"
    echo "  --latency     네트워크 지연 시뮬레이션 (10ms ± 5ms)"
    echo "  --stop        컨테이너 중지 및 삭제"
    echo "  --clean       컨테이너 및 이미지 모두 삭제"
    echo ""
    echo "Examples:"
    echo "  $0 --basic"
    echo "  $0 --latency"
    echo "  $0 --stop"
}

# 컨테이너 중지
stop_containers() {
    echo "Stopping containers..."
    docker-compose down
    docker-compose -f docker-compose-with-latency.yml down
    echo "Containers stopped."
}

# 클린업
clean_all() {
    echo "Cleaning up containers and images..."
    docker-compose down --rmi all --volumes
    docker-compose -f docker-compose-with-latency.yml down --rmi all --volumes
    echo "Cleanup complete."
}

# 기본 테스트
run_basic_test() {
    echo "Starting basic test (no latency)..."
    echo ""

    # 컨테이너 빌드 및 시작
    echo "Building and starting containers..."
    docker-compose up -d --build

    # Service A가 준비될 때까지 대기
    echo ""
    echo "Waiting for services to be ready..."
    sleep 20

    # Health check
    echo "Checking service health..."
    if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
        echo "ERROR: Service A is not healthy"
        exit 1
    fi

    if ! curl -s http://localhost:8081/actuator/health > /dev/null; then
        echo "ERROR: Service B is not healthy"
        exit 1
    fi

    echo "✓ All services are healthy"
    echo ""

    # 테스트 실행
    echo "Running performance test..."
    echo "This may take a few minutes..."
    echo ""

    curl -X POST "http://localhost:8080/api/test/compare?totalCount=400000&batchSize=1000"

    echo ""
    echo ""
    echo "======================================"
    echo "Test completed!"
    echo "======================================"
    echo ""
    echo "Check results:"
    echo "  cat docs/response.md"
    echo ""
    echo "View logs:"
    echo "  docker-compose logs service-a"
    echo "  docker-compose logs service-b"
    echo ""
    echo "Stop containers:"
    echo "  ./docker-test.sh --stop"
}

# 네트워크 지연 테스트
run_latency_test() {
    echo "Starting latency test (10ms ± 5ms)..."
    echo ""

    # 컨테이너 빌드 및 시작
    echo "Building and starting containers with toxiproxy..."
    docker-compose -f docker-compose-with-latency.yml up -d --build

    # Service A가 준비될 때까지 대기
    echo ""
    echo "Waiting for services to be ready..."
    sleep 25

    # Health check
    echo "Checking service health..."
    if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
        echo "ERROR: Service A is not healthy"
        exit 1
    fi

    echo "✓ All services are healthy"
    echo ""

    # Toxiproxy 상태 확인
    echo "Toxiproxy configuration:"
    curl -s http://localhost:8474/proxies | python3 -m json.tool || echo "Toxiproxy running"
    echo ""

    # 테스트 실행
    echo "Running performance test with network latency..."
    echo "This may take a few minutes..."
    echo ""

    curl -X POST "http://localhost:8080/api/test/compare?totalCount=400000&batchSize=1000"

    echo ""
    echo ""
    echo "======================================"
    echo "Test completed!"
    echo "======================================"
    echo ""
    echo "Check results:"
    echo "  cat docs/response.md"
    echo ""
    echo "Compare with basic test results to see the impact of network latency"
    echo ""
    echo "View logs:"
    echo "  docker-compose -f docker-compose-with-latency.yml logs service-a"
    echo "  docker-compose -f docker-compose-with-latency.yml logs service-b"
    echo ""
    echo "Stop containers:"
    echo "  ./docker-test.sh --stop"
}

# 인자 파싱
case "$1" in
    --basic)
        run_basic_test
        ;;
    --latency)
        run_latency_test
        ;;
    --stop)
        stop_containers
        ;;
    --clean)
        clean_all
        ;;
    *)
        usage
        exit 1
        ;;
esac