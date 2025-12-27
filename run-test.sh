#!/bin/bash

echo "Running HTTP vs gRPC Performance Test"
echo "=============================================="

# Default values
TOTAL_COUNT=${1:-400000}
BATCH_SIZE=${2:-1000}

echo "Test Parameters:"
echo "  - Total Count: $TOTAL_COUNT"
echo "  - Batch Size: $BATCH_SIZE"
echo ""

# Check if services are running
if ! curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "ERROR: Service A is not running on port 8080"
    echo "Please start services first: ./start-services.sh"
    exit 1
fi

if ! curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo "ERROR: Service B is not running on port 8081"
    echo "Please start services first: ./start-services.sh"
    exit 1
fi

echo "Both services are healthy. Starting performance test..."
echo ""

# Run the comparison test
echo "Calling: POST http://localhost:8080/api/test/compare?totalCount=$TOTAL_COUNT&batchSize=$BATCH_SIZE"
echo ""

RESPONSE=$(curl -s -X POST "http://localhost:8080/api/test/compare?totalCount=$TOTAL_COUNT&batchSize=$BATCH_SIZE")

echo "Response:"
echo "$RESPONSE"
echo ""

# Check if response-basic-multiple.md was created
if [ -f "docs/response.md" ]; then
    echo "=============================================="
    echo "Test completed! Results saved to docs/response.md"
    echo ""
    echo "View results with: cat docs/response.md"
    echo "Or open in your editor: open docs/response.md"
else
    echo "WARNING: docs/response.md was not created. Check the logs for errors."
fi