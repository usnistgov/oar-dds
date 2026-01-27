# Config Server Setup

This directory contains the Spring Cloud Config Server setup for OAR-DDS microservices.

**The JAR file contains baked-in configuration with secrets (API keys, passwords, etc.).**

- The JAR file is **NOT committed to git**

## Prerequisites

- Java 21+ (for building the JAR)
- Maven 3.9+
- Git access to [usnistgov/oar-config](https://github.com/usnistgov/oar-config)
- Docker (for running)

## Quick Setup

```bash
# 1. Build the JAR (config files are baked in)
./setup-config.sh

# 2. Build and run with Docker
docker build -t oar-config-server .
docker run -p 8888:8888 oar-config-server

# Or use Docker Compose from project root
docker-compose up -d config-server
```

## Verify

```bash
curl http://localhost:8888/actuator/health
curl http://localhost:8888/cache-mgmt-service/local
```

## Updating Config

To update with a different branch:

```bash
./setup-config.sh develop/oar-dds-config
docker-compose build config-server
docker-compose up -d config-server
```

## How It Works

The setup script:
1. Clones/updates the oar-config repository
2. Builds the config server JAR with Maven
3. Config files are baked into the JAR at build time (via `classpath:` paths)
4. Copies only the JAR to this directory
