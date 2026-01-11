#!/bin/bash
#
# Start all OAR microservices using Docker Compose
#
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}Starting OAR microservices...${NC}"
echo ""

# Check if config server is running externally
echo -e "${YELLOW}Checking config server...${NC}"
if curl -s --max-time 3 http://localhost:8888/actuator/health 2>/dev/null | grep -q "UP"; then
    echo -e "${GREEN}Config server is running on localhost:8888${NC}"
else
    echo -e "${YELLOW}Note: Config server not detected at localhost:8888${NC}"
    echo "Start it with: cd /path/to/oar-config-server && java -jar target/oar-config-server-*.jar --server.port=8888"
    echo ""
fi

# Start Docker Compose services
echo -e "${YELLOW}Starting Docker Compose services...${NC}"
docker-compose up --build -d

echo ""
echo -e "${GREEN}Services starting...${NC}"
echo ""
echo "Monitor startup with:"
echo "  docker-compose ps"
echo "  docker-compose logs -f"
echo ""
echo "Check service health at:"
echo "  Eureka:      http://localhost:8761"
echo "  API Gateway: http://localhost:8080"
echo "  Demo UI:     http://localhost:4200"
echo ""
