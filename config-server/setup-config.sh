#!/bin/bash
#
# Setup script for OAR-DDS Config Server
#
# This script clones/updates the oar-config repository, builds the config server JAR,
# and copies it to this directory. Config files are baked into the JAR.
#
# Usage:
#   ./setup-config.sh [branch]
#
# Arguments:
#   branch  - Git branch to checkout (default: develop/oar-dds-config)
#
# Examples:
#   ./setup-config.sh                           # Use default branch
#   ./setup-config.sh develop/oar-dds-config    # Specify branch
#

set -e

# Configuration
REPO_URL="https://github.com/usnistgov/oar-config.git"
DEFAULT_BRANCH="develop/oar-dds-config"
TEMP_DIR="/tmp/oar-config-setup"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Parse arguments
BRANCH="${1:-$DEFAULT_BRANCH}"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  OAR-DDS Config Server Setup${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "Repository: ${YELLOW}${REPO_URL}${NC}"
echo -e "Branch:     ${YELLOW}${BRANCH}${NC}"
echo -e "Target:     ${YELLOW}${SCRIPT_DIR}${NC}"
echo ""

# Check prerequisites
echo -e "${BLUE}[1/4] Checking prerequisites...${NC}"

if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed. Please install Java 21+${NC}"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed. Please install Maven 3.9+${NC}"
    exit 1
fi

if ! command -v git &> /dev/null; then
    echo -e "${RED}Error: Git is not installed.${NC}"
    exit 1
fi

echo -e "${GREEN}Prerequisites OK${NC}"
echo ""

# Clone or update repository
echo -e "${BLUE}[2/4] Cloning/updating repository...${NC}"

if [ -d "$TEMP_DIR" ]; then
    echo "Updating existing clone..."
    cd "$TEMP_DIR"
    git fetch --all
    git checkout "$BRANCH" || {
        echo -e "${RED}Error: Branch '$BRANCH' not found${NC}"
        echo "Available branches:"
        git branch -r | grep -v HEAD | sed 's/origin\//  /'
        exit 1
    }
    git pull origin "$BRANCH"
else
    echo "Cloning repository..."
    git clone "$REPO_URL" "$TEMP_DIR"
    cd "$TEMP_DIR"
    git checkout "$BRANCH" || {
        echo -e "${RED}Error: Branch '$BRANCH' not found${NC}"
        echo "Available branches:"
        git branch -r | grep -v HEAD | sed 's/origin\//  /'
        exit 1
    }
fi

echo -e "${GREEN}Repository ready${NC}"
echo ""

# Build the JAR
echo -e "${BLUE}[3/4] Building config server JAR...${NC}"

cd "$TEMP_DIR/oar-config-server"
mvn clean package -DskipTests -q

JAR_FILE=$(ls target/oar-config-server-*.jar 2>/dev/null | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}Error: JAR file not found after build${NC}"
    exit 1
fi

echo -e "${GREEN}Build successful: $(basename $JAR_FILE)${NC}"
echo ""

# Copy JAR file
echo -e "${BLUE}[4/4] Copying JAR file...${NC}"

# Remove old JAR files
rm -f "$SCRIPT_DIR"/oar-config-server-*.jar

# Copy new JAR
cp "$JAR_FILE" "$SCRIPT_DIR/"
echo -e "${GREEN}Copied: $(basename $JAR_FILE)${NC}"
echo ""

# Summary
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}  Setup Complete!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "JAR file: $(basename $JAR_FILE)"
echo "(Config files are baked into the JAR)"
echo ""
echo -e "${YELLOW}To build and run with Docker:${NC}"
echo ""
echo "  cd $SCRIPT_DIR"
echo "  docker build -t oar-config-server ."
echo "  docker run -p 8888:8888 oar-config-server"
echo ""
echo -e "${YELLOW}Or with Docker Compose (from project root):${NC}"
echo ""
echo "  docker-compose up -d config-server"
echo ""
echo -e "${YELLOW}To verify:${NC}"
echo "  curl http://localhost:8888/actuator/health"
echo ""
