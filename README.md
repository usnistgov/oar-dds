# OAR-DDS: Data Distribution System

Microservices implementation of the NIST OAR Data Distribution System for scalable, resilient data distribution.

## Project Structure

```
oar-dds/
├── common/                          # Shared client libraries
│   ├── cache-manager-client/        # Feign client for cache-mgmt service
│   └── dataset-access-client/       # Feign client for dataset-access service
│
├── infrastructure/                  # Infrastructure services
│   ├── api-gateway/                 # Spring Cloud Gateway
│   └── eureka-server/               # Service discovery
│
├── services/                        # Business microservices
│   ├── dataset-access/              # Dataset file downloads
│   ├── aip-access/                  # AIP bag access
│   ├── bundle-plan/                 # Bundle planning
│   ├── data-bundle/                 # Bundle downloads
│   ├── cache-mgmt/                  # Cache operations
│   ├── restricted-access/           # RPA requests
│   └── version-service/             # Version info
│
├── docker-compose.yml               # Docker orchestration
├── demo.sh                          # CLI for testing workflows
└── pom.xml                          # Parent POM
```

## Services Overview

### Infrastructure Services

| Service | Purpose |
|---------|---------|
| Eureka Server | Service registry and discovery |
| API Gateway | Unified entry point, routing, load balancing |

### Business Services

| Service | Gateway Route | Purpose |
|---------|---------------|---------|
| dataset-access | `/od/ds/**` | Serves dataset files from cache or preservation bags |
| aip-access | `/aip/**` | Archive Information Package access |
| bundle-plan | `/bundle/plan/**` | Creates download plans, validates URLs |
| data-bundle | `/bundle/data/**` | Streams files into zip bundles |
| cache-mgmt | `/cache/**` | Cache volumes, metadata, object management |
| restricted-access | `/rpa/**` | Restricted Public Access request handling |
| version-service | `/version/**` | Build and version information |

## Quick Start with Docker

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker and Docker Compose
- External `oar-config-server` repository (with config files in `config/oar-dds/`)

### Build and Run

```bash
# Build all services
mvn clean package -DskipTests

# Start external config server first (from oar-config-server repo)
cd /path/to/oar-config-server
java -jar target/oar-config-server-1.2.0.jar --server.port=8888 &

# Start Docker services
cd /path/to/oar-dds
docker-compose up -d

# Check status
./demo.sh status

# Stop Docker services
docker-compose down
```

### Using demo.sh CLI

The `demo.sh` script provides commands for testing and demonstration:

```bash
./demo.sh help              # Show available commands
./demo.sh status            # Check all services health
./demo.sh workflow          # Test dataset-access ↔ cache-mgmt flow
./demo.sh bundle            # Test bundle plan and download workflow
./demo.sh cache             # Inspect cache volumes and contents
./demo.sh cache-clear       # Clear all cached files
./demo.sh urls              # Show all service URLs
./demo.sh logs [service]    # View service logs
```

## API Examples

### Download a File

```bash
# Via API Gateway
curl http://localhost:8080/od/ds/mds1491/small-1kb.dat -o file.dat

# Direct to service
curl http://localhost:8081/ds/mds1491/small-1kb.dat -o file.dat
```

### Get Dataset Metadata

```bash
curl http://localhost:8080/cache/metadata/mds1491 | jq .
```

### Check Cache Status

```bash
# List cache volumes
curl http://localhost:8080/cache/volumes/ | jq .

# Check cached objects for a dataset
curl http://localhost:8080/cache/objects/mds1491 | jq .
```

### Create Bundle Plan

```bash
curl -X POST http://localhost:8080/bundle/plan/ds/_bundle_plan \
  -H "Content-Type: application/json" \
  -d '{
    "bundleName": "my-download",
    "includeFiles": [
      {
        "filePath": "mds1491/small-1kb.dat",
        "downloadUrl": "http://api-gateway:8080/od/ds/mds1491/small-1kb.dat",
        "fileSize": 1369
      },
      {
        "filePath": "mds1491/medium-1mb.dat",
        "downloadUrl": "http://api-gateway:8080/od/ds/mds1491/medium-1mb.dat",
        "fileSize": 1398105
      }
    ]
  }' | jq .
```

### Download Bundle

```bash
curl -X POST http://localhost:8080/bundle/data/ds/_bundle \
  -H "Content-Type: application/json" \
  -d '{
    "bundleName": "my-download",
    "includeFiles": [
      {
        "filePath": "mds1491/small-1kb.dat",
        "downloadUrl": "http://api-gateway:8080/od/ds/mds1491/small-1kb.dat",
        "fileSize": 1369
      },
      {
        "filePath": "mds1491/medium-1mb.dat",
        "downloadUrl": "http://api-gateway:8080/od/ds/mds1491/medium-1mb.dat",
        "fileSize": 1398105
      }
    ]
  }' -o bundle.zip

# Verify contents
unzip -l bundle.zip
```

## Architecture

### Inter-Service Communication

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│   Client    │────────►│ API Gateway │────────►│   Service   │
│             │         │   (8080)    │         │             │
└─────────────┘         └─────────────┘         └─────────────┘
                               │
                               ▼
                        ┌─────────────┐
                        │   Eureka    │
                        │   (8761)    │
                        └─────────────┘
```

### Bundle Download Flow

```
1. Client → POST /bundle/plan/_bundle_plan
   - bundle-plan validates URLs (HEAD requests)
   - Returns plan with file list, sizes, bundle splits

2. Client → POST /bundle/data/_bundle
   - data-bundle fetches files via api-gateway
   - Streams zip to client with proper directory structure
```

### File Download Flow

```
1. Client → GET /od/ds/{dataset}/{file}
2. API Gateway → routes to dataset-access
3. dataset-access → calls cache-mgmt (Feign client)
4. cache-mgmt → checks cache, restores from bag if needed
5. Response streams back through the chain
```

## Configuration

Service configurations are managed by an external Config Server (separate `oar-config-server` repository).

### External Config Server Setup

The config server runs locally (not in Docker) and Docker services connect via `host.docker.internal:8888`.

Config files are stored in:
- `oar-config-server/src/main/resources/config/oar-dds/`

Key configuration files:
- `cache-management-service.yml` - Cache volumes, preservation bag paths
- `dataset-access-service.yml` - Remote cache manager settings
- `bundle-plan-service.yml` - Bundle size limits, database settings
- `data-bundle-service.yml` - Packaging limits, allowed URLs
- `restricted-access-service.yml` - RPA settings, JWT configuration

### Starting the Config Server

```bash
# From oar-config-server directory
cd /path/to/oar-config-server
java -jar target/oar-config-server-1.2.0.jar --server.port=8888

# Verify config server is running
curl http://localhost:8888/actuator/health
```

The config server must be running before starting Docker services.

## Docker Services

```bash
# View running containers
docker-compose ps

# View logs for specific service
docker logs oar-ms-cache-mgmt -f

# Execute command in container
docker exec oar-ms-postgres psql -U oar_app -d oar_cache -c "SELECT * FROM volumes;"

# Rebuild single service
docker-compose build cache-mgmt
docker-compose up -d cache-mgmt
```

## Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Java | OpenJDK | 17 |
| Framework | Spring Boot | 3.3.0 |
| Cloud | Spring Cloud | 2023.0.3 |
| Service Discovery | Eureka | 2023.0.3 |
| API Gateway | Spring Cloud Gateway | 2023.0.3 |
| Inter-Service | OpenFeign | 2023.0.3 |
| Database | PostgreSQL | 15-alpine |
| Cache | Redis | 7-alpine |
| Build | Maven | 3.9+ |
| Container | Docker | Latest |

## Example Python Client

```python
import requests

# Via API Gateway (recommended)
GATEWAY = "http://localhost:8080"
resp = requests.get(f"{GATEWAY}/od/ds/mds1491/small-1kb.dat")
metadata = requests.get(f"{GATEWAY}/cache/metadata/mds1491").json()

# Direct to service (bypassing gateway)
resp = requests.get("http://localhost:8081/ds/mds1491/small-1kb.dat")
volumes = requests.get("http://localhost:8085/cache/volumes/").json()

# Bundle download (downloadUrl must use internal Docker network)
bundle_req = {
    "bundleName": "my-bundle",
    "includeFiles": [
        {"filePath": "mds1491/small-1kb.dat",
         "downloadUrl": "http://api-gateway:8080/od/ds/mds1491/small-1kb.dat",
         "fileSize": 1369}
    ]
}
resp = requests.post(f"{GATEWAY}/bundle/data/ds/_bundle", json=bundle_req)
```

## Troubleshooting

### Services fail to start (config server connection)

Ensure the external config server is running before starting Docker services:
```bash
# Check config server is running
curl http://localhost:8888/actuator/health

# If not running, start it
cd /path/to/oar-config-server
java -jar target/oar-config-server-1.2.0.jar --server.port=8888 &
```

Docker services connect via `host.docker.internal:8888`.

### Service not registering with Eureka

Check that the service name in `bootstrap.yml` matches the config file name:
```yaml
spring:
  application:
    name: cache-management-service  # Must match config file name
```

### Gateway returning 404

Verify route configuration in `api-gateway/application.yml` and check that target service is registered in Eureka.

### Bundle download fails

Ensure `downloadUrl` uses Docker internal network:
```json
{
  "downloadUrl": "http://api-gateway:8080/od/ds/mds1491/file.dat"
}
```
Not browser URL like `http://localhost:8080/...`

### Cache miss for existing file

Check preservation bag path in config and verify bag exists in mounted volume:
```bash
docker exec oar-ms-cache-mgmt ls -la /data/preservation-bags/
```

## Disclaimer

NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.
