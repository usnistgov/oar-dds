#!/bin/bash
#
# Clean and reset all services and data
#

# Colors
YELLOW='\033[1;33m'
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

echo -e "${YELLOW}======================================"
echo "Clean & Reset OAR Microservices"
echo "======================================${NC}"
echo ""
echo -e "${RED}WARNING: This will:${NC}"
echo "  • Stop all running containers"
echo "  • Remove all containers"
echo "  • Remove all Docker volumes (database data will be lost)"
echo "  • Clean Maven build artifacts"
echo ""
read -p "Are you sure? (yes/no): " -r
echo ""

if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    echo "Cancelled."
    exit 1
fi

echo "Stopping and removing containers..."
docker-compose down -v

echo ""
echo "Removing Docker images..."
docker-compose down --rmi local 2>/dev/null || true

echo ""
echo "Cleaning Maven build artifacts..."
mvn clean

echo ""
echo "Removing dangling Docker volumes..."
docker volume prune -f

echo ""
echo -e "${GREEN}======================================"
echo "Cleanup complete!"
echo "======================================${NC}"
echo ""
echo "System is now in a clean state."
echo "Run ./build-all.sh to rebuild."
echo ""
