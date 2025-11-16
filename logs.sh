#!/bin/bash
#
# View logs for a specific service or all services
#

SERVICE=${1:-}

if [ -z "$SERVICE" ]; then
    echo "Usage: ./logs.sh [service-name]"
    echo ""
    echo "Available services:"
    echo "  postgres"
    echo "  config-server"
    echo "  eureka-server"
    echo "  api-gateway"
    echo "  dataset-access"
    echo "  aip-access"
    echo "  bundle-plan"
    echo "  data-bundle"
    echo "  cache-mgmt"
    echo "  restricted-access"
    echo "  version-service"
    echo ""
    echo "To view all logs: ./logs.sh all"
    exit 1
fi

if [ "$SERVICE" = "all" ]; then
    docker-compose logs -f
else
    docker-compose logs -f "$SERVICE"
fi
