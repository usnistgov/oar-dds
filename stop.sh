#!/bin/bash
#
# Stop all OAR microservices
#
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Stopping OAR microservices...${NC}"
echo ""

docker-compose down

echo ""
echo -e "${GREEN}All services stopped.${NC}"
echo ""
echo "To also remove volumes (database data): docker-compose down -v"
echo "To remove images: docker-compose down --rmi all"
echo ""
