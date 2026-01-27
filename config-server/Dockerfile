# OAR-DDS Config Server Dockerfile
#
# Build:   docker build -t oar-config-server .
# Run:     docker run -p 8888:8888 oar-config-server

FROM eclipse-temurin:21-jre-alpine

# Create oarop user (consistent with other OAR containers)
RUN addgroup -S oarop && adduser -S oarop -G oarop

# Setup directories
RUN mkdir -p /tmp/log/config-server && \
    ln -s /tmp/log/config-server /var/log/config-server && \
    chmod -R a+rx /tmp/log && \
    chown oarop:oarop /tmp/log/config-server

RUN mkdir -p /usr/local/oar-config-server

# Copy JAR (config files are baked inside)
COPY oar-config-server-*.jar /usr/local/oar-config-server/oar-config-server.jar
COPY run.sh /usr/local/bin/run-server.sh

RUN chmod a+rx /usr/local/bin/run-server.sh && \
    chown -R oarop:oarop /usr/local/oar-config-server

USER oarop

EXPOSE 8888

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8888/actuator/health || exit 1

CMD ["/usr/local/bin/run-server.sh"]
