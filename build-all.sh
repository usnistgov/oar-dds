#!/bin/bash
#
# Build all microservices JARs
#
set -e

echo "======================================"
echo "Building all microservices..."
echo "======================================"

# Build parent and all modules
echo "Building all modules..."
mvn clean package -DskipTests

echo ""
echo "======================================"
echo "Build complete!"
echo "======================================"
echo ""
echo "JAR files created:"
find . -name "*.jar" -path "*/target/*" ! -name "*-sources.jar" ! -name "*-javadoc.jar" | while read jar; do
    size=$(du -h "$jar" | cut -f1)
    echo "  $jar ($size)"
done

echo ""
echo "Ready to run: docker-compose up --build"
