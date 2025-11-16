#!/bin/bash
#
# Stop all microservices
#
echo "Stopping all services..."
docker-compose down

echo ""
echo "All services stopped."
echo ""
echo "To remove volumes (database data): docker-compose down -v"
