#!/bin/bash

echo "Starting HTTP vs gRPC Performance Test Services"
echo "=============================================="

# Check if gradle wrapper exists
if [ ! -f "service-a/gradlew" ] || [ ! -f "service-b/gradlew" ]; then
    echo "Gradle wrapper not found. Creating wrapper files..."

    # Create basic gradle wrapper files if gradle is installed
    if command -v gradle &> /dev/null; then
        echo "Creating gradle wrapper for service-a..."
        cd service-a && gradle wrapper --gradle-version 8.5 && cd ..

        echo "Creating gradle wrapper for service-b..."
        cd service-b && gradle wrapper --gradle-version 8.5 && cd ..
    else
        echo "WARNING: Gradle is not installed. Please install Gradle first or use an IDE to build."
        echo "Visit: https://gradle.org/install/"
        exit 1
    fi
fi

# Build both services
echo ""
echo "Building Service B (Server)..."
cd service-b
./gradlew clean build -x test
if [ $? -ne 0 ]; then
    echo "Failed to build Service B"
    exit 1
fi
cd ..

echo ""
echo "Building Service A (Client)..."
cd service-a
./gradlew clean build -x test
if [ $? -ne 0 ]; then
    echo "Failed to build Service A"
    exit 1
fi
cd ..

echo ""
echo "=============================================="
echo "Build completed successfully!"
echo ""
echo "Starting Service B (Server) on port 8081 (HTTP) and 9091 (gRPC)..."
cd service-b
./gradlew bootRun > ../service-b.log 2>&1 &
SERVICE_B_PID=$!
cd ..

echo "Service B started with PID: $SERVICE_B_PID"
echo "Waiting for Service B to initialize..."
sleep 10

echo ""
echo "Starting Service A (Client) on port 8080..."
cd service-a
./gradlew bootRun > ../service-a.log 2>&1 &
SERVICE_A_PID=$!
cd ..

echo "Service A started with PID: $SERVICE_A_PID"
echo "Waiting for Service A to initialize..."
sleep 10

echo ""
echo "=============================================="
echo "Both services are running!"
echo ""
echo "Service B (Server):"
echo "  - HTTP: http://localhost:8081"
echo "  - gRPC: localhost:9091"
echo "  - PID: $SERVICE_B_PID"
echo "  - Logs: service-b.log"
echo ""
echo "Service A (Client):"
echo "  - HTTP: http://localhost:8080"
echo "  - PID: $SERVICE_A_PID"
echo "  - Logs: service-a.log"
echo ""
echo "To run performance test:"
echo "  curl -X POST 'http://localhost:8080/api/test/compare?totalCount=400000&batchSize=1000'"
echo ""
echo "To stop services:"
echo "  kill $SERVICE_B_PID $SERVICE_A_PID"
echo ""
echo "Or run: ./stop-services.sh"
echo ""
echo "Process IDs saved to .service-pids"
echo "$SERVICE_B_PID" > .service-pids
echo "$SERVICE_A_PID" >> .service-pids