#!/bin/sh

echo "********************************************************"
echo "Starting Configuration Service"
echo "********************************************************"

java -Xms256m -Xmx512m \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    -Djava.security.egd=file:/dev/./urandom \
    -jar /usr/local/oar-config-server/oar-config-server.jar \
    --server.port=8888
