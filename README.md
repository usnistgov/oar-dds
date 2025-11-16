# OAR Distribution Service - Microservices Architecture

Microservices implementation of the NIST OAR Distribution Service for scalable, resilient data distribution.

## 🎯 **Project Status**

**Phase:** Foundation Setup
**Version:** 1.0.0-SNAPSHOT

### ✅ Completed (Phase 1)
- [x] Project structure created
- [x] Parent POM with Spring Boot 3.3.0 & Spring Cloud 2023.0.3
- [x] Common modules (shared-models, shared-exceptions, shared-utils)
- [x] Git ignore configuration

### 🚧 In Progress
- [ ] Infrastructure services (Eureka, Config Server, Gateway)
- [ ] Microservices extraction
- [ ] Docker orchestration

---

## 📁 **Project Structure**

```
oar-dist-ms/
├── common/                      # Shared libraries
│   ├── shared-models/           # DTOs, domain objects
│   ├── shared-exceptions/       # Exception classes
│   └── shared-utils/            # Utility functions
│
├── infrastructure/              # Infrastructure services
│   ├── eureka-server/           # Service discovery
│   ├── config-server/           # Centralized configuration
│   └── api-gateway/             # API Gateway & routing
│
├── services/                    # Business microservices
│   ├── dataset-access/          # Dataset file downloads
│   ├── aip-access/              # AIP bag access
│   ├── cache-management/        # Cache operations
│   ├── bundle-download-plan/    # Bundle planning
│   ├── data-bundle-access/      # Bundle downloads
│   ├── restricted-access/       # RPA requests
│   └── version-service/         # Version info
│
├── docker/                      # Docker configurations
├── config-repo/                 # Config Server repository
├── pom.xml                      # Parent POM
├── .gitignore                   # Git ignore rules
├── README.md                    # This file
└── MICROSERVICES_ARCHITECTURE.md  # Detailed architecture doc
```

---

## 🛠️ **Technology Stack**

| Component | Technology | Version |
|-----------|-----------|---------|
| **Java** | OpenJDK | 17 |
| **Framework** | Spring Boot | 3.3.0 |
| **Cloud** | Spring Cloud | 2023.0.3 |
| **Service Discovery** | Eureka | 2023.0.3 |
| **API Gateway** | Spring Cloud Gateway | 2023.0.3 |
| **Config Management** | Spring Cloud Config | 2023.0.3 |
| **Circuit Breaker** | Resilience4j | 2.1.0 |
| **Tracing** | Zipkin + Micrometer | Latest |
| **Database** | PostgreSQL | 15-alpine |
| **Build** | Maven | 3.9+ |
| **Container** | Docker | Latest |

---

## 🚀 **Quick Start**

### Prerequisites

```bash
java -version    # OpenJDK 17
mvn --version    # Maven 3.9+
docker --version # Docker latest
```

### Build Common Modules

```bash
cd /Users/one1/oar-dist-ms

# Build all common modules
mvn clean install -pl common/shared-models,common/shared-exceptions,common/shared-utils -am
```

### Next Steps

**Phase 2:** Build infrastructure services
```bash
# Coming next:
# - Eureka Server
# - Config Server
# - API Gateway
```

---

## 📖 **Documentation**

- **[Architecture Document](MICROSERVICES_ARCHITECTURE.md)** - Complete system design
- **Build Instructions** - Coming soon
- **Deployment Guide** - Coming soon
- **API Documentation** - Coming soon

---

## 🏗️ **Microservices Overview**

### Infrastructure Services

| Service | Port | Purpose |
|---------|------|---------|
| **Eureka Server** | 8761 | Service registry & discovery |
| **Config Server** | 8888 | Centralized configuration |
| **API Gateway** | 8080 | Single entry point, routing |

### Business Services

| Service | Port | Base Path | Purpose |
|---------|------|-----------|---------|
| **Dataset Access** | 8081 | /od/ds/** | File downloads |
| **AIP Access** | 8082 | /aip/** | AIP bag retrieval |
| **Cache Management** | 8085 | /cache/** | Cache operations |
| **Bundle Plan** | 8083 | /bundle/plan/** | Bundle planning |
| **Bundle Access** | 8084 | /bundle/download/** | Bundle downloads |
| **Restricted Access** | 8086 | /rpa/** | RPA requests |
| **Version Service** | 8087 | /version/** | Version info |

---

## 🔧 **Development**

### Build All Modules

```bash
mvn clean install
```

### Build Specific Module

```bash
mvn clean install -pl common/shared-models
```

### Run Tests

```bash
mvn test
```

---

## 🐳 **Docker Deployment**

*Coming in Phase 9*

```bash
# Build images
docker-compose build

# Start all services
docker-compose up

# Access services
curl http://localhost:8080/actuator/health
```

---

## 📊 **Monitoring & Observability**

*Coming in Phase 13*

- **Eureka Dashboard:** http://localhost:8761
- **Zipkin UI:** http://localhost:9411
- **Prometheus:** http://localhost:9090
- **Grafana:** http://localhost:3000

---

## 🤝 **Contributing**

### Module Guidelines

**Common Modules:**
- **shared-models**: DTOs and domain objects only
- **shared-exceptions**: Exception classes only
- **shared-utils**: Pure utility functions, no business logic

**Services:**
- Each service owns its data (database-per-service pattern)
- Inter-service communication via REST APIs (Feign)
- Include health checks and metrics

---

## 📝 **License**

This software was developed at the National Institute of Standards and Technology by employees of the Federal Government in the course of their official duties.

---

## 📞 **Contact**

For questions or issues, please refer to the architecture document or contact the development team.

---

**Last Updated:** 2025-01-15
**Status:** Phase 1 Complete ✅
