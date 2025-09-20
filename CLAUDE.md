# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EPIK (Every Place In Korea) is a Spring Boot 3.3.4 backend API serving a Korean cultural events platform. It aggregates information about pop-up stores, concerts, exhibitions, and musicals, with integration to the KOPIS (Korean performing arts information system) API.

## Build System & Common Commands

**Build Tool**: Maven with wrapper
**Java Version**: 21

### Essential Commands
```bash
# Run the application (serves on http://localhost:8081/api/v1)
./mvnw spring-boot:run

# Compile the project
./mvnw clean compile

# Run all tests
./mvnw test

# Build JAR package
./mvnw clean package

# Clean build artifacts
./mvnw clean
```

## Project Architecture

### Package Structure
The codebase follows a domain-driven design with role-based access control:

```
com.everyplaceinkorea.epik_boot3_api/
├── admin/          # Admin panel operations (백오피스)
├── anonymous/      # Public APIs (no authentication)
├── auth/           # Authentication & authorization
├── config/         # Spring configuration classes
├── entity/         # JPA entities
├── external/       # External API integrations (KOPIS)
├── member/         # Authenticated user operations
└── repository/     # Data access layer
```

### Core Domains
- **Musical**: Performance management with tickets, images, bookmarks
- **Popup**: Pop-up store information with regional categorization
- **Concert**: Concert event management
- **Exhibition**: Exhibition event management
- **Member**: User management and social login
- **Feed**: User-generated content

### External Integrations
- **KOPIS API**: Korean performing arts data synchronization (daily at 2 AM)
- **OAuth2**: Google, Kakao, Naver social login
- **Email**: Gmail SMTP for notifications

## Database Configuration

**Primary Database**: MySQL
- **Connection**: `jdbc:mysql://127.0.0.1:3306/epik_local`
- **ORM**: Spring Data JPA with DDL auto-update
- **Alternative**: H2 configuration available (commented out in application.yml)

## Authentication & Security

- **JWT-based authentication** with 24-hour token expiration
- **Role-based access control** via package structure (`admin/`, `anonymous/`, `member/`)
- **Spring Security** with OAuth2 client configuration
- **Environment variables** in `.env` file for sensitive data

## Development Configuration

### Key Configuration Files
- **Application Config**: `src/main/resources/application.yml`
- **Environment Variables**: `.env` (database credentials, OAuth2 keys, JWT secrets)
- **Custom Bean Naming**: Uses `CustomBeanNameGenerator` for component scanning

### Development Features
- **DevTools**: Enabled for hot reload during development
- **File Upload**: 10MB max file size, 30MB max request
- **Static Resources**: Served from `uploads/images/` and `uploads/temp/`
- **Context Path**: `/api/v1`
- **Server Port**: 8081

## Testing

**Framework**: JUnit 5
**Location**: `src/test/java/`
**Current Coverage**: Basic context loading test only

## KOPIS API Integration

The application integrates with Korea's performing arts information system:
- **Service**: `KopisApiService` and `KopisDataSyncService`
- **Scheduled Tasks**: Daily synchronization at 2 AM using Quartz scheduler
- **Data Types**: Performance lists, details, facility information
- **Headers**: Custom User-Agent and Accept headers for API stability

## File Structure Notes

- **Main Application**: `EpikBoot3ApiApplication.java`
- **Custom Configuration**: Package scanning with custom bean naming
- **Upload Management**: Structured directory organization for images and temporary files
- **Logging**: Comprehensive logging configuration for debugging

## Development Workflow

1. **Environment Setup**: Ensure `.env` file contains required database and OAuth2 credentials
2. **Database**: Start MySQL service and ensure `epik_local` database exists
3. **Run Application**: Use `./mvnw spring-boot:run` for development
4. **API Testing**: Access endpoints at `http://localhost:8081/api/v1`
5. **Hot Reload**: DevTools automatically reloads on code changes

## Key Dependencies

- **Spring Boot 3.3.4** (Web, Data JPA, Security, OAuth2, WebFlux, Quartz)
- **JWT**: `jjwt` 0.11.5 for token handling
- **Database**: MySQL Connector, optional H2 for testing
- **Utilities**: Lombok, ModelMapper, Jackson XML/JSON
- **External APIs**: WebClient for reactive HTTP calls