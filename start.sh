#!/bin/bash
#
# Start all microservices with docker-compose
#
set -e

# Load environment variables if .env file exists
if [ -f .env ]; then
    echo "Loading environment from .env file..."
    export $(cat .env | grep -v '^#' | xargs)
fi

# Default profile to local if not set
export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-local}

echo "======================================"
echo "Starting OAR Microservices"
echo "Profile: $SPRING_PROFILES_ACTIVE"
echo "======================================"

# Start services
docker-compose up -d

echo ""
echo "Services starting... checking status:"
echo ""

sleep 5

docker-compose ps

echo ""
echo "======================================"
echo "Services started!"
echo "======================================"
echo ""
echo "Key URLs:"
echo "  Config Server:  http://localhost:8888"
echo "  Eureka Server:  http://localhost:8761"
echo "  API Gateway:    http://localhost:8080"
echo "  Dataset Access: http://localhost:8081"
echo "  AIP Access:     http://localhost:8082"
echo "  Bundle Plan:    http://localhost:8083"
echo "  Data Bundle:    http://localhost:8084"
echo "  Cache Mgmt:     http://localhost:8085"
echo "  RPA Service:    http://localhost:8086"
echo "  Version:        http://localhost:8087"
echo ""
echo "To view logs: docker-compose logs -f [service-name]"
echo "To stop:      docker-compose down"
