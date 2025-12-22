# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EMS (Energy Management System) is a Spring Boot-based application for monitoring and managing radiation and environmental devices. The system includes device data collection, user management, company management, and real-time data processing capabilities.

## Architecture

- **Backend**: Spring Boot 3.5.0 with Java 17
- **Database**: H2 in-memory database (for development/testing)
- **Security**: Spring Security with JWT authentication
- **Documentation**: Swagger/OpenAPI 3
- **Testing**: JUnit 5 with Spring Boot Test
- **Containerization**: Docker with multi-service setup

### Key Components

- **Device Management**: Handles registration and management of radiation (RADIATION) and environmental (ENVIRONMENT) devices
- **Data Collection**: Receives device data via REST APIs (device-data endpoints)
- **User Management**: Role-based access control (ADMIN, USER)
- **Company Management**: Multi-tenant company structure
- **Authentication**: JWT-based stateless authentication

## Development Commands

### Backend Development

```bash
# Navigate to backend directory
cd backend

# Build the project
mvn clean compile

# Run tests
mvn test

# Run a specific test class
mvn test -Dtest=DeviceDataReceiverControllerTest

# Run a specific test method
mvn test -Dtest=DeviceDataReceiverControllerTest#testReceiveRadiationDataSuccess

# Build JAR package
mvn clean package

# Run the application (development mode)
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Skip tests during build
mvn clean package -DskipTests
```

### Docker Development

```bash
# Start all services (backend, mosquitto, nodered)
docker-compose up -d

# Build and start services
docker-compose up -d --build

# View service logs
docker-compose logs ems-backend
docker-compose logs nodered
docker-compose logs mosquitto

# Stop all services
docker-compose down

# Completely reset (remove volumes)
docker-compose down -v

# Execute commands in containers
docker exec -it ems-backend bash
docker exec -it ems-nodered bash
```

## Project Structure

### Backend Package Organization

- `com.cdutetc.ems.controller`: REST API endpoints
- `com.cdutetc.ems.service`: Business logic layer
- `com.cdutetc.ems.repository`: Data access layer (JPA)
- `com.cdutetc.ems.entity`: Database entities
- `com.cdutetc.ems.dto`: Data transfer objects (request/response)
- `com.cdutetc.ems.security`: Security configuration and JWT handling
- `com.cdutetc.ems.exception`: Global exception handling
- `com.cdutetc.ems.config`: Configuration classes

### Key Entity Relationships

- **Company**: Top-level organization entity
- **User**: Users belong to companies, have roles (ADMIN/USER)
- **Device**: Devices belong to companies, have types (RADIATION/ENVIRONMENT)
- **RadiationDeviceData**: Time-series data from radiation devices
- **EnvironmentDeviceData**: Time-series data from environmental devices

## Configuration

### Application Profiles

- **Default**: Development with H2 in-memory database
- **Test**: Test configuration with clean database state
- **Docker**: Production-like configuration with environment variables

### Security Configuration

- JWT secret and expiration can be configured via environment variables:
  - `EMS_SECURITY_JWT_SECRET`: JWT signing secret
  - `EMS_SECURITY_JWT_EXPIRATION`: Token expiration time in milliseconds

### Database Configuration

- **Development**: H2 in-memory database with console enabled
- **Console Access**: http://localhost:8081/api/h2-console
- **JDBC URL**: `jdbc:h2:mem:ems-test-db`
- **Credentials**: username `sa`, password empty

## API Endpoints

### Public Endpoints (No Authentication)

- `POST /api/auth/login`: User authentication
- `POST /api/device-data/radiation`: Receive radiation device data
- `POST /api/device-data/environment`: Receive environment device data
- `/actuator/health`: Health check endpoint

### Protected Endpoints (Authentication Required)

- Device management: `/api/devices/**`
- User management: `/api/users/**`
- Company management: `/api/companies/**`

## Testing Strategy

### Test Data Management

- Uses `TestDataBuilder` for creating consistent test data
- `BaseIntegrationTest` provides common test configuration
- Database is reset between tests (`create-drop`)

### Test Categories

- **Unit Tests**: Service layer business logic
- **Integration Tests**: API endpoints with database interaction
- **Security Tests**: Authentication and authorization

## Device Data Flow

1. **Device Registration**: Devices must be registered via API before data submission
2. **Data Reception**: Raw device data submitted to `/api/device-data/*` endpoints
3. **Data Processing**: System validates and processes incoming data
4. **Data Storage**: Validated data stored in appropriate entity tables
5. **Error Handling**: Invalid data from unregistered devices is rejected

## Docker Services

### Service Ports

- **EMS Backend**: 8081 (API), context path `/api`
- **Node-RED**: 1880 (device simulator and flow editor)
- **Mosquitto MQTT**: 1883 (MQTT broker)
- **H2 Console**: 8081/api/h2-console (via backend)

### Service Dependencies

- Node-RED depends on Mosquitto (for MQTT communication)
- Node-RED depends on EMS Backend (for HTTP data submission)
- All services communicate via shared Docker network

## Development Notes

### Code Conventions

- Uses Lombok for reducing boilerplate code
- follows standard Spring Boot patterns
- Implements DTO pattern for API request/response
- Uses proper HTTP status codes and error handling

### Environment Variables

The application supports Docker environment variable overrides for:
- Database configuration
- JWT settings
- Server port and context path
- Management endpoints

### Common Development Workflows

1. **Local Development**: Use `mvn spring-boot:run` with H2 console
2. **Testing**: Run `mvn test` for unit/integration tests
3. **Integration Testing**: Use `docker-compose up` for full system testing
4. **API Development**: Access Swagger UI at http://localhost:8081/swagger-ui.html

## Troubleshooting

### Common Issues

- **Port Conflicts**: Ensure ports 8081, 1880, 1883 are available
- **Database Issues**: H2 console available for database inspection
- **Authentication**: Check JWT configuration and token expiration
- **Device Data**: Verify device registration before data submission