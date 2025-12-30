# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EMS (Energy Management System) is a full-stack monitoring platform for radiation and environmental devices. The system features real-time data collection via MQTT/HTTP, intelligent alerting, and regulatory data reporting to government platforms (Shandong and Sichuan protocols).

**Tech Stack**:
- **Backend**: Spring Boot 3.5.9 + Java 17 + MySQL + Redis
- **Frontend**: Vue 3 + Vite + Element Plus + Pinia
- **Message Queue**: Mosquitto MQTT Broker
- **Device Simulator**: Node-RED

## Development Commands

### Backend (Spring Boot + Maven)

```bash
cd backend

# Build and compile
mvn clean compile

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=DeviceServiceTest

# Run specific test method
mvn test -Dtest=DeviceServiceTest#testGetDeviceById

# Build package (skip tests for speed)
mvn clean package -DskipTests

# Run application (development)
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Key test files**:
- `DeviceCacheSyncServiceTest` - Cache synchronization tests
- `MonitoringDataBufferServiceTest` - Write-behind buffer tests
- `DeviceServiceTest` - Device business logic tests

### Frontend (Vue 3 + Vite)

```bash
cd frontend

# Install dependencies
npm install

# Run development server (http://localhost:5173)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

**Frontend proxy configuration**: The Vite dev server proxies `/api` requests to `http://localhost:8080` (see [vite.config.js](frontend/vite.config.js:14-18)).

### Docker Services

```bash
# Start all services (MySQL, Redis, Mosquitto, Node-RED)
docker-compose up -d

# Rebuild and start
docker-compose up -d --build

# View logs
docker-compose logs ems-backend
docker-compose logs mysql
docker-compose logs redis

# Stop services
docker-compose down

# Stop and remove volumes (complete reset)
docker-compose down -v

# Execute in container
docker exec -it ems-backend bash
docker exec -it ems-mysql mysql -u ems_user -pems_pass ems_db
docker exec -it ems-redis redis-cli -a ems_redis_pass
```

**Service Ports**:
- EMS Backend: `8080` (API), context path `/api`
- MySQL: `3306`
- Redis: `6379`
- Mosquitto MQTT: `1883`
- Node-RED: `1880`
- H2 Console: `8080/api/h2-console` (when enabled)

## Core Architecture

### Backend Package Structure

```
com.cdutetc.ems/
├── controller/         # REST API endpoints
├── service/            # Business logic layer
│   ├── impl/          # Service implementations
│   └── report/        # Data reporting services (Shandong/Sichuan)
├── repository/         # JPA data access layer
├── entity/            # Database entities (Device, User, AlertRecord, etc.)
├── dto/               # Request/Response DTOs
├── security/          # JWT + Spring Security configuration
├── mqtt/              # MQTT message listener
├── scheduler/         # Scheduled tasks (data flush, alerts)
├── config/            # Configuration classes (Redis, Async, etc.)
└── util/              # Utility classes
```

### Performance Optimization Architecture

The system uses a **Write-Behind caching strategy** for high-throughput data ingestion:

1. **Real-time Layer**: Incoming device data stored in Redis for immediate query
2. **Buffer Layer**: Data accumulated in memory queues for batch processing
3. **Persistence Layer**: Scheduled tasks batch-flush data to MySQL every 60 seconds

**Key Components**:
- `MonitoringDataBufferService` ([backend/src/main/java/com/cdutetc/ems/service/MonitoringDataBufferService.java](backend/src/main/java/com/cdutetc/ems/service/MonitoringDataBufferService.java)) - Manages write-behind buffer
- `DeviceCacheSyncService` ([backend/src/main/java/com/cdutetc/ems/service/DeviceCacheSyncService.java](backend/src/main/java/com/cdutetc/ems/service/DeviceCacheSyncService.java)) - Syncs device cache to Redis
- `MonitoringDataFlushScheduler` ([backend/src/main/java/com/cdutetc/ems/scheduler/MonitoringDataFlushScheduler.java](backend/src/main/java/com/cdutetc/ems/scheduler/MonitoringDataFlushScheduler.java)) - Scheduled batch flush

### Cache Layer Design

**Redis is used for**:
- **Device status cache** - Real-time online/offline status (TTL: 15 minutes)
- **Device metadata cache** - Device configurations (TTL: 30 minutes)
- **Real-time data cache** - Latest device measurements (TTL: 5 minutes)
- **Alert cache** - Recent alert records (TTL: 10 minutes)

**Cache services**:
- `DeviceCacheService` - Device metadata caching
- `DeviceStatusCacheService` - Device online status caching
- `AlertCacheService` - Alert record caching

### Data Flow

**MQTT Data Ingestion**:
1. Device publishes to topic: `ems/device/{deviceId}/data/{TYPE}`
2. `MqttMessageListener` receives and validates data
3. Unregistered devices are auto-registered to default company (ID: 1)
4. Data flows through `MonitoringDataBufferService` (Redis + batch queue)
5. `MonitoringDataFlushScheduler` batch-flushes to MySQL every 60s

**REST API Data Ingestion**:
1. Device POSTs to `/api/device-data/radiation` or `/api/device-data/environment`
2. Device must be pre-registered (returns 404 if not found)
3. Same buffering and persistence flow as MQTT

### Alert System

**Alert Types**:
1. **CPM Rise Alert** - Triggers when CPM increases by >15% (configurable per device type)
2. **Low Battery Alert** - Triggers when voltage drops below threshold:
   - Radiation devices: < 3.7V
   - Environment devices: < 11.1V (3 cells)
3. **Device Offline Alert** - Triggers after 10 minutes without data

**Configuration** ([application.yaml](backend/src/main/resources/application.yaml:131-144)):
```yaml
app:
  ems:
    alert:
      cpm-rise:
        radiation-rise-percentage: 0.15  # 15% threshold
        environment-rise-percentage: 0.15
        min-interval: 300  # 5 minutes between alerts
        min-cpm: 50  # Minimum CPM to avoid false positives
      low-battery:
        radiation-threshold: 3.7  # Volts
        environment-threshold: 11.1  # Volts
      offline-timeout:
        timeout-minutes: 10
```

### Data Reporting (Regulatory Protocols)

**Shandong Protocol** (TCP + HJ/T212-2005):
- Server: `221.214.62.118:20050`
- Service: `ShandongDataReportService`
- Implementation: `HJT212ProtocolService`

**Sichuan Protocol** (HTTP + SM2 Encryption):
- Server: `http://59.225.208.12:18085`
- Service: `SichuanDataReportService`
- Encryption: `Sm2EncryptionService` (BouncyCastle)

**Configuration** ([application.yaml](backend/src/main/resources/application.yaml:146-176)):
```yaml
app:
  ems:
    data-report:
      shandong:
        host: 221.214.62.118
        port: 20050
        enabled: true
      sichuan:
        url: http://59.225.208.12:18085/access/data/report
        enabled: true
```

### CPM Conversion Factors

The system applies device-specific CPM (Counts Per Minute) conversion factors:

**Configuration** ([application.yaml](backend/src/main/resources/application.yaml:111-115)):
```yaml
app:
  ems:
    mqtt:
      cpm:
        radiation-conversion-factor: 10.0    # Raw value ÷ 10 = Standard CPM
        environment-conversion-factor: 634.0  # Raw value ÷ 634 = Standard CPM
```

**Implementation**: Raw CPM values are divided by these factors before storage and alerting.

## Important Configuration Details

### Environment Variables

The application supports Docker environment variable overrides:

```bash
# JWT Security
EMS_SECURITY_JWT_SECRET=your-secret-key
EMS_SECURITY_JWT_EXPIRATION=86400000

# MQTT Configuration
EMS_MQTT_HOST=mosquitto  # Docker service name
EMS_MQTT_PORT=1883
EMS_DEFAULT_COMPANY_ID=1

# Redis Configuration
EMS_REDIS_HOST=redis
EMS_REDIS_PORT=6379
EMS_REDIS_PASSWORD=ems_redis_pass

# Alert Thresholds
EMS_ALERT_CPM_RADIATION_RISE=0.15
EMS_ALERT_RADIATION_VOLTAGE=3.7

# Data Reporting
EMS_REPORT_SHANDONG_ENABLED=true
EMS_REPORT_SICHUAN_ENABLED=true
```

### Database Schema

**Key Entities**:
- `Device` - Device registry with type (RADIATION/ENVIRONMENT)
- `RadiationDeviceData` - Time-series radiation measurements
- `EnvironmentDeviceData` - Time-series environment measurements
- `AlertRecord` - Alert history
- `User` - User accounts with role-based access (ADMIN/USER)
- `Company` - Multi-tenant organization

**Relationships**: Company → Users + Devices (One-to-Many)

### Frontend Structure

```
frontend/src/
├── api/           # Axios API clients
├── components/    # Reusable Vue components
├── views/         # Page-level components
├── router/        # Vue Router configuration
├── store/         # Pinia state management
├── utils/         # Utility functions
└── constants/     # Application constants
```

**Key Frontend Features**:
- Real-time dashboard with SSE (Server-Sent Events)
- Device management and monitoring
- Alert history and configuration
- Data export and reporting

## Testing Strategy

### Test Categories

**Unit Tests**: Service layer business logic
**Integration Tests**: API endpoints with database interaction
**Cache Tests**: Redis caching and synchronization

### Running Tests

```bash
# Backend tests
cd backend
mvn test

# Specific test
mvn test -Dtest=MonitoringDataBufferServiceTest

# Integration test with Docker
docker-compose up -d
mvn test -Dspring.profiles.active=integration
```

### Test Data Management

- `BaseIntegrationTest` - Common test configuration
- Database reset between tests (`create-drop`)
- Redis keys cleared before each test
- Test data builders available in `util/` package

## Development Notes

### Code Conventions

- **Backend**: Lombok annotations, standard Spring Boot patterns, DTO pattern for APIs
- **Frontend**: Vue 3 Composition API with `<script setup>`, Element Plus components
- **Encoding**: UTF-8 across all files
- **Timezone**: Asia/Shanghai (GMT+8)

### Common Development Workflows

1. **Feature Development**:
   - Backend: Create/modify entities → repositories → services → controllers
   - Frontend: Create API client → Pinia store → Vue components
   - Tests: Write unit tests for services, integration tests for controllers

2. **Cache Changes**:
   - Modify cache service → update TTL in `@Cacheable` annotations
   - Test with `DeviceCacheSyncServiceTest` to verify sync behavior

3. **Data Reporting Changes**:
   - Protocol changes in `service/report/` package
   - Test with Python scripts in `tests/protocol/`

### Troubleshooting

**Port Conflicts**:
```bash
# Check port usage on macOS
lsof -i :8080  # Backend
lsof -i :5173  # Frontend
lsof -i :1883  # MQTT
```

**MQTT Connection Issues**:
```bash
# Check Mosquitto logs
docker-compose logs mosquitto

# Test MQTT connection
telnet localhost 1883
```

**Cache Issues**:
```bash
# Connect to Redis CLI
docker exec -it ems-redis redis-cli -a ems_redis_pass

# View all keys
KEYS ems:*

# Clear device cache
DEL ems:device:*
```

**Database Issues**:
```bash
# Check MySQL logs
docker-compose logs mysql

# Connect to MySQL
docker exec -it ems-mysql mysql -u ems_user -pems_pass ems_db

# Check slow queries
SHOW VARIABLES LIKE 'slow_query_log';
```

## Additional Documentation

- [System Architecture](docs/design/系统架构设计.md) - Overall system design
- [API Documentation](docs/design/API接口文档.md) - REST API details
- [Data Flow](docs/design/数据流处理说明.md) - Data collection and processing
- [Deployment Guide](docs/design/部署指南.md) - Production deployment
- [Protocol Docs](docs/protocol/) - Shandong/Sichuan protocol specifications
