# Repository Guide

## Toolchain

- This is one Java 21 / Spring Boot 4.1 Maven module; use the checked-in wrapper (`mvnw.cmd` on Windows, `./mvnw` on Unix).
- `verify` is the full gate: tests, Spotless check, Google Checkstyle (including tests), and SpotBugs with `Max` effort / `Low` threshold. Run `./mvnw verify` before finishing.
- Spotless does not auto-format during `verify`. Apply its Google Java Format rules with `./mvnw spotless:apply`, then rerun `verify`.
- Focus tests with `./mvnw -Dtest=ActivityServiceTest test` or `./mvnw -Dtest=ActivityServiceTest#createsActivityAndMapsWeatherConditions test`.
- Start locally with `./mvnw spring-boot:run`; the API listens on port 8080. `docker compose up --build` builds the image and exposes the same port.

## Application Shape

- `com.solnotfound.ServerApplication` is the application entrypoint; the current HTTP surface is `/activities` in `controller/ActivityController`.
- Request validation is split: Jakarta annotations on DTO records cover field constraints, while cross-field rules live in `service/ActivityService`. Keep both paths and their error behavior covered when changing validation.
- API validation failures use `ProblemDetail` through `exception/GlobalExceptionHandler`.
- `repository/ActivityRepository` is intentionally an in-memory `ConcurrentHashMap`; data disappears on restart. MongoDB dependencies exist in `pom.xml`, but no MongoDB persistence is wired yet. Do not assume MongoDB is required by the current tests or runtime.
- Existing tests are lightweight unit tests that instantiate controllers, services, and repositories directly; they do not start Spring, MongoDB, Docker, or Testcontainers.

## Build Details

- The Docker builder runs `./mvnw clean verify`, so formatting, Checkstyle, SpotBugs, or test failures also break `docker compose build`.
- Java sources must use Unix line endings because Spotless explicitly enforces them.

## Documentation

- Every new non-trivial method must include Javadoc describing its behavior, relevant business
  rules, side effects, return value, and exceptional cases. Do not add redundant Javadoc to simple
  getters, setters, constructors, or direct delegations.
