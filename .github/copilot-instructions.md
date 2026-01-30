# Spring Dashboarding Project - AI Coding Instructions

## Project Overview
This is a Spring Boot 4.0.2 dashboarding application using **Java 25** with PostgreSQL, organized as a modular monolith using **Spring Modulith**. The architecture emphasizes module boundaries through package structure.

## Architecture & Module Structure

### Module Organization
- **Package = Module boundary**: Each package under `nl.kabisa.dashboarding` represents a distinct module
- Current modules:
  - `restservice`: REST API endpoints (example: greeting service)
  - `dashboard`: JPA entity management for dashboards with PostgreSQL/JSONB
- **Spring Modulith enforces module boundaries** - modules communicate via well-defined interfaces, not direct package access

### Key Technologies
- Spring Boot 4.0.2 with Spring Modulith 2.0.2
- Java 25 (uses modern features like records)
- PostgreSQL 16 with JSONB support
- JPA/Hibernate with automatic schema updates
- Maven wrapper for builds (use `./mvnw`, not global Maven)

## Development Workflows

### Database Setup
```bash
docker compose up # Starts PostgreSQL container
```
Database credentials in [application.properties](src/main/resources/application.properties): `learninguser/learningpass` on `localhost:5432/learningdb`

### Running the Application
```bash
./mvnw spring-boot:run
```
Or use VS Code's "Run: DashboardingApplication" terminal configuration.

### Testing
```bash
./mvnw test
```
- Use `@SpringBootTest` with `@AutoConfigureMockMvc` for integration tests
- Example: [GreetingControllerTest.java](src/test/java/nl/kabisa/dashboarding/restservice/GreetingControllerTest.java)

## Code Conventions & Patterns

### REST Controllers
- Place in module-specific packages (e.g., `restservice`)
- Use `@RestController` with `@GetMapping`/`@PostMapping`
- Example pattern in [GreetingController.java](src/main/java/nl/kabisa/dashboarding/restservice/GreetingController.java)

### DTOs & Value Objects
- **Use Java records for immutable DTOs**: `public record Greeting(long id, String content)`
- See [Greeting.java](src/main/java/nl/kabisa/dashboarding/restservice/Greeting.java) for reference

### JPA Entities
- Standard JPA annotations with lifecycle callbacks (`@PrePersist`, `@PreUpdate`)
- Use `@JdbcTypeCode(SqlTypes.JSON)` with `columnDefinition = "jsonb"` for PostgreSQL JSON columns
- UUID primary keys with `@GeneratedValue(strategy = GenerationType.UUID)`
- Soft deletes via `deletedAt` timestamps
- Example: [Dashboard.java](src/main/java/nl/kabisa/dashboarding/dashboard/Dashboard.java)

### Repositories
- Extend `JpaRepository<Entity, ID>` interface
- Mark with `@Repository` annotation
- Example: [DashboardRepository.java](src/main/java/nl/kabisa/dashboarding/dashboard/DashboardRepository.java)

## Important Notes
- **Always use `./mvnw`** (Maven wrapper) instead of system Maven for consistency
- Database schema auto-updates via `spring.jpa.hibernate.ddl-auto=update` - suitable for learning, not production
- Spring DevTools enabled for automatic reloading during development
