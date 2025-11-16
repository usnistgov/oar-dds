#!/bin/bash
#
# Complete demo script for OAR Microservices
#
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}======================================"
echo "OAR Microservices Demo"
echo "======================================${NC}"
echo ""

# Step 1: Build
echo -e "${YELLOW}Step 1: Building all microservices...${NC}"
echo "This will compile all Java code and create JAR files."
echo ""
read -p "Press Enter to continue..."
./build-all.sh

echo ""
echo -e "${GREEN}✓ Build complete!${NC}"
echo ""
sleep 2

# Step 2: Start services
echo -e "${YELLOW}Step 2: Starting all services with Docker Compose...${NC}"
echo "This will start PostgreSQL, Config Server, Eureka, API Gateway, and all microservices."
echo ""
read -p "Press Enter to continue..."
./start.sh

echo ""
sleep 5

# Step 3: Check status
echo -e "${YELLOW}Step 3: Checking service status...${NC}"
echo ""
read -p "Press Enter to continue..."
./status.sh

echo ""
sleep 2

# Step 4: Show Eureka Dashboard
echo -e "${YELLOW}Step 4: Service Discovery Dashboard${NC}"
echo "Opening Eureka Dashboard in browser..."
echo "You can see all registered microservices here."
echo ""
echo "URL: http://localhost:8761"
read -p "Press Enter to continue..."

# Step 5: Test API Gateway
echo ""
echo -e "${YELLOW}Step 5: Testing API Gateway${NC}"
echo "The API Gateway routes requests to microservices."
echo ""
echo "Available routes:"
echo "  /dataset/** -> Dataset Access Service"
echo "  /aip/**     -> AIP Access Service"
echo "  /bundle/**  -> Bundle Plan Service"
echo ""
read -p "Press Enter to test the version endpoint..."

echo ""
echo "Testing: http://localhost:8080/version/info"
curl -s http://localhost:8080/version/info | jq '.' || echo "Response received (install jq for formatted output)"

echo ""
echo -e "${GREEN}======================================"
echo "Demo Complete!"
echo "======================================${NC}"
echo ""
echo "Next steps:"
echo "  • View logs:    docker-compose logs -f [service-name]"
echo "  • Stop all:     ./stop.sh"
echo "  • Clean reset:  ./clean.sh"
echo ""
echo "For detailed guide, see: DEMO_GUIDE.md"
echo ""
