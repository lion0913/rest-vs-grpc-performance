#!/bin/bash

echo "Stopping HTTP vs gRPC Performance Test Services"
echo "=============================================="

if [ -f .service-pids ]; then
    echo "Reading service PIDs from .service-pids..."

    while IFS= read -r pid; do
        if ps -p $pid > /dev/null 2>&1; then
            echo "Stopping process $pid..."
            kill $pid
        else
            echo "Process $pid not found (already stopped)"
        fi
    done < .service-pids

    rm .service-pids
    echo "All services stopped."
else
    echo "No .service-pids file found."
    echo "Searching for running Java processes..."

    # Find and kill service-a and service-b processes
    pkill -f "service-a.*bootRun"
    pkill -f "service-b.*bootRun"

    echo "Attempted to stop services via pkill."
fi

echo ""
echo "Done!"