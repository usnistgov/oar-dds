#!/bin/bash
#
# Check status of all microservices
#

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}======================================"
echo "OAR Microservices Status"
echo "======================================${NC}"
echo ""

# Check Docker Compose services
echo -e "${YELLOW}Docker Container Status:${NC}"
docker-compose ps
echo ""

# Check Eureka registrations
echo -e "${YELLOW}Services Registered with Eureka:${NC}"
if curl -s http://localhost:8761/eureka/apps > /dev/null 2>&1; then
    SERVICES=$(curl -s http://localhost:8761/eureka/apps | grep -o '<app>[^<]*</app>' | sed 's/<app>//;s/<\/app>//' | sort | uniq)
    if [ -z "$SERVICES" ]; then
        echo -e "${RED}No services registered yet. They may still be starting up...${NC}"
    else
        echo "$SERVICES" | while read service; do
            echo -e "  ${GREEN}✓${NC} $service"
        done
    fi
else
    echo -e "${RED}✗ Cannot connect to Eureka (http://localhost:8761)${NC}"
    echo "  Eureka may still be starting up..."
fi
echo ""

# Check service health endpoints
echo -e "${YELLOW}Service Health Checks:${NC}"

check_health() {
    SERVICE=$1
    PORT=$2
    URL="http://localhost:$PORT/actuator/health"
    
    if curl -s "$URL" > /dev/null 2>&1; then
        STATUS=$(curl -s "$URL" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        if [ "$STATUS" = "UP" ]; then
            echo -e "  ${GREEN}✓${NC} $SERVICE (port $PORT) - UP"
        else
            echo -e "  ${YELLOW}!${NC} $SERVICE (port $PORT) - $STATUS"
        fi
    else
        echo -e "  ${RED}✗${NC} $SERVICE (port $PORT) - No response"
    fi
}

check_health "Config Server" 8888
check_health "Eureka Server" 8761
check_health "Dataset Access" 8081
check_health "AIP Access" 8082
check_health "Bundle Plan" 8083
check_health "Data Bundle" 8084
check_health "Cache Management" 8085
check_health "Restricted Access" 8086
check_health "Version Service" 8087

echo ""
echo -e "${BLUE}======================================"
echo "Key URLs:"
echo "======================================${NC}"
echo "  Eureka Dashboard:     http://localhost:8761"
echo "  Config Server:        http://localhost:8888"
echo "  API Gateway:          http://localhost:8080"
echo "  API Gateway Swagger:  http://localhost:8080/swagger-ui.html"
echo ""
echo "  Dataset Access:       http://localhost:8081"
echo "  AIP Access:           http://localhost:8082"
echo "  Bundle Plan:          http://localhost:8083"
echo "  Data Bundle:          http://localhost:8084"
echo "  Cache Management:     http://localhost:8085"
echo "  Restricted Access:    http://localhost:8086"
echo "  Version Service:      http://localhost:8087"
echo ""
