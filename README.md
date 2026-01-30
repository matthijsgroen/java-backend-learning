# Spring Dashboarding Application

A Spring Boot 4.0.2 dashboarding application using Java 25 with PostgreSQL, organized as a modular monolith using Spring Modulith.

## Quick Start

### Prerequisites
- Java 25+
- Docker (for PostgreSQL)
- Maven Wrapper (included)

### Setup

1. **Start the database:**
   ```bash
   docker compose up
   ```

2. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```
   The application will be available at `http://localhost:8080`

3. **Run tests:**
   ```bash
   ./mvnw test
   ```

### API Documentation

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/api-docs

## Project Structure

```
src/main/java/nl/kabisa/dashboarding/
├── restservice/        # REST API endpoints (greeting service)
└── dashboard/          # Dashboard entity & controller
```

## Technology Stack

* **Framework:** Spring Boot 4.0.2 with Spring Modulith 2.0.2
* **Language:** Java 25
* **Database:** PostgreSQL 16 with JSONB support
* **ORM:** JPA/Hibernate
* **Documentation:** Springdoc OpenAPI 3.0.0

## Key Features

- **Module Boundaries:** Package-based module organization with Spring Modulith
- **JSON Support:** Jackson databind for automatic JSON/JSONB mapping
- **Soft Deletes:** Dashboard entities support soft deletion via `deletedAt` timestamps
- **UUID Primary Keys:** All entities use UUID identifiers
- **Auto-Documentation:** OpenAPI specification auto-generated from code

## Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.2/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.0.2/maven-plugin/build-image.html)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/4.0.2/reference/using/devtools.html)
* [Spring Modulith](https://docs.spring.io/spring-modulith/reference/)
* [Spring Web](https://docs.spring.io/spring-boot/4.0.2/reference/web/servlet.html)
* [Springdoc OpenAPI](https://springdoc.org/)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

