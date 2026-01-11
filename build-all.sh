#!/bin/bash
#
# Build all OAR microservices
#
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}Building all OAR microservices...${NC}"
echo ""

# Build from root pom (builds all modules)
echo -e "${YELLOW}Running Maven build...${NC}"
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}Build completed successfully!${NC}"
    echo ""
    echo "JAR files created:"
    find . -name "*.jar" -path "*/target/*" -type f 2>/dev/null | grep -v "original" | grep -v "sources" | head -20
else
    echo ""
    echo -e "${RED}Build failed!${NC}"
    exit 1
fi
