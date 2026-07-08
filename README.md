# spring-webflux-tutorial

A complete, hands-on walkthrough of building a **reactive** REST API with **Spring Boot 3.5.16** (Spring Framework 6, Java 17) and **Spring WebFlux**, organized into Git branches that progressively cover most of the key concepts of reactive programming in the Spring ecosystem.

The data model follows the `spring-boot-tutorial` tutorial: `categories` → `products` → `orders` ← `customers`.

This document is the **complete specification** of the project: it is meant to be followed step by step to implement each branch.

## Table of contents

- [Why a dedicated reactive tutorial](#why-a-dedicated-reactive-tutorial)
- [Reactive programming fundamentals](#reactive-programming-fundamentals)
- [Tech stack](#tech-stack)
- [Data model](#data-model)
- [Branching strategy](#branching-strategy)
- [Project structure](#project-structure)
- [Standard response format](#standard-response-format)
- [Spring AOP in a reactive context](#spring-aop-in-a-reactive-context)
- [feature/core-architecture](#featurecore-architecture)
- [feature/products](#featureproducts)
- [feature/customers](#featurecustomers)
- [feature/orders](#featureorders)
- [feature/auth](#featureauth)
- [Order of work](#order-of-work)
- [Code conventions](#code-conventions)
- [Concepts covered](#concepts-covered)
- [How to follow this tutorial](#how-to-follow-this-tutorial)

## Why a dedicated reactive tutorial

Spring WebFlux changes several habits carried over from `spring-boot-tutorial` (blocking, Spring MVC + JPA):

- **Spring Data JPA → Spring Data R2DBC**: no more Hibernate proxies, no automatic lazy loading; every relation (`Product → Category`, `Order → Customer/Product`) must be resolved explicitly with reactive operators (`flatMap`, `zip`)
- **`Page<T>` → manual pagination**: R2DBC does not natively support `Pageable` on repositories, so explicit `LIMIT`/`OFFSET` queries have to be written
- **`@Valid` behaves differently depending on the API style**: automatic with annotated controllers, manual (via an injected `Validator`) with functional endpoints
- **Spring AOP** must respect the reactive chain (never block a `Mono`/`Flux` inside an aspect)
- The HTTP flow itself can be an infinite/continuous stream (Server-Sent Events)

## Reactive programming fundamentals

Explaining reactive programming itself, not just how to wire WebFlux, is the actual goal of this tutorial. Spring WebFlux is built on **Project Reactor**, an implementation of the **Reactive Streams** specification (a four-interface contract: `Publisher`, `Subscriber`, `Subscription`, `Processor`) for asynchronous stream processing with non-blocking backpressure.

- **`Mono<T>`**: a publisher that emits at most one element (or an error) - used for single-resource operations (`findById`, `save`, a single downstream HTTP call).
- **`Flux<T>`**: a publisher that emits zero to many elements over time - used for collections/lists and for continuous streams (SSE).
- **Nothing happens until subscription**: a `Mono`/`Flux` is a declarative, reusable recipe. No query runs and no HTTP call is made until something subscribes to it. Spring subscribes automatically when a controller returns a `Mono`/`Flux`, which is exactly what makes naive logging around a reactive call misleading (see [Spring AOP in a reactive context](#spring-aop-in-a-reactive-context) for a concrete case where this bites).
- **Backpressure**: a subscriber signals how many elements it can handle right now (`request(n)`); a producer emitting faster than that is required to wait rather than overwhelm the consumer. Spring MVC has no equivalent concept, since it processes one blocking request per thread with no notion of "too much, too fast."
- **Non-blocking I/O and the event-loop model**: the embedded server is **Netty**, running a small, fixed number of event-loop threads instead of the one-thread-per-request model used by Tomcat in Spring MVC. A thread is never held waiting on I/O (a database call, an HTTP call): it is released back to the loop and resumed later when data actually arrives. This is exactly why `.block()` anywhere in application code (see [Code conventions](#code-conventions)) defeats the whole model: it ties up an event-loop thread, which can stall unrelated requests being served by the same loop.
- **Schedulers**: `subscribeOn`/`publishOn` control which thread pool executes upstream/downstream operators; `Schedulers.boundedElastic()` exists for the rare, unavoidable blocking call. None should be needed on the golden path of this tutorial - it is a documented escape hatch, not something to reach for by default.
- **Cold vs. hot publishers**: repository and `WebClient` calls are **cold** (a fresh call is made per subscriber, nothing is shared). The `Sinks.Many` used for the order status stream (`feature/orders`) is **hot** and multicast: every currently subscribed client receives the same live events as they happen, rather than each triggering their own independent stream.
- **Operators as a pipeline, not a loop**: `map`, `flatMap`, `zip`, `switchIfEmpty`, `onErrorResume` compose a data pipeline declaratively; there is no `for` loop pulling elements one by one, and no shared mutable state to reason about between steps.

## Tech stack

| Component | Choice |
|---|---|
| Framework | Spring Boot 3.5.16 (Spring Framework 6) |
| Language | Java 17 (LTS) |
| Build | Maven |
| Server | Netty (embedded, non-blocking) |
| Database | PostgreSQL 16 (via Docker Compose) |
| Reactive data access | Spring Data R2DBC (`r2dbc-postgresql`) |
| Migrations | Flyway (dedicated JDBC connection, migrations are always blocking at startup) |
| DTO mapping | MapStruct + Lombok |
| Validation | Jakarta Bean Validation (automatic with annotated, manual with functional) |
| API documentation | springdoc-openapi (WebFlux support, Swagger UI) |
| Monitoring | Spring Boot Actuator |
| Security | Spring Security Reactive + JWT (jjwt) |
| Aspect-oriented programming | Spring AOP (adapted to reactive types) |
| Reactive HTTP client | `WebClient` |
| Scheduled tasks | Spring Scheduling (`@Scheduled`) |
| Tests | JUnit 5, `StepVerifier`, `WebTestClient`, Testcontainers (R2DBC) |
| CI/CD | GitHub Actions |
| Containerization | Docker, docker-compose |

## Data model

```
categories (id, category_name)
    │ 1
    │
    │ N
products (id, category_id, product_name, unit_price)
    │ 1
    │
    │ N
orders (id, customer_id, product_id, quantity, total)
    │ N
    │
    │ 1
customers (id, first_name, last_name, telephone, email, address)
```

Columns and constraints are identical to `spring-boot-tutorial` (see its README). The difference lies solely in how these relations are **resolved on the application side** (no JPA `@OneToMany`/`@ManyToOne`: each R2DBC entity is independent, joins are done through explicit queries or composed in the service).

## Branching strategy

| Branch | Role |
|---|---|
| `master` | Stable, production-ready code. No direct commits, only merges from `develop`. |
| `develop` | Integration branch. All `feature/*` branches are merged here before `master`. |
| `feature/core-architecture` | Reactive technical foundation: project structure, R2DBC/Flyway configuration, Docker, CI. |
| `feature/products` | Reactive `Category`/`Product` CRUD, annotated controller style. |
| `feature/customers` | Reactive `Customer` CRUD, implemented as **functional endpoints** (`RouterFunction`) to contrast with the annotated style. |
| `feature/orders` | Reactive `Order` CRUD + real-time order status stream (SSE). |
| `feature/auth` | Reactive security (Spring Security WebFlux + JWT). |

## Project structure

```
spring-webflux-tutorial/
├── src/
│   ├── main/
│   │   ├── java/edgareldy/springwebfluxtutorial/
│   │   │   ├── SpringWebfluxTutorialApplication.java
│   │   │   ├── config/
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── R2dbcConfig.java
│   │   │   │   ├── WebClientConfig.java
│   │   │   │   ├── CorsConfig.java
│   │   │   │   └── SchedulingConfig.java
│   │   │   ├── entity/
│   │   │   │   ├── Category.java
│   │   │   │   ├── Product.java
│   │   │   │   ├── Customer.java
│   │   │   │   ├── Order.java
│   │   │   │   └── user/
│   │   │   │       ├── AppUser.java
│   │   │   │       └── Role.java
│   │   │   ├── repository/
│   │   │   │   ├── CategoryRepository.java     (ReactiveCrudRepository)
│   │   │   │   ├── ProductRepository.java      (R2dbcRepository)
│   │   │   │   ├── CustomerRepository.java
│   │   │   │   ├── OrderRepository.java
│   │   │   │   └── AppUserRepository.java
│   │   │   ├── dto/
│   │   │   │   ├── common/
│   │   │   │   │   ├── ApiResponse.java
│   │   │   │   │   └── PageResponse.java
│   │   │   │   ├── category/ (CategoryRequest, CategoryResponse)
│   │   │   │   ├── product/ (ProductRequest, ProductResponse)
│   │   │   │   ├── customer/ (CustomerRequest, CustomerResponse)
│   │   │   │   ├── order/ (OrderRequest, OrderResponse, OrderStatusEvent)
│   │   │   │   └── auth/ (RegisterRequest, LoginRequest, AuthResponse)
│   │   │   ├── mapper/
│   │   │   │   ├── CategoryMapper.java
│   │   │   │   ├── ProductMapper.java
│   │   │   │   ├── CustomerMapper.java
│   │   │   │   └── OrderMapper.java
│   │   │   ├── service/
│   │   │   │   ├── CategoryService.java
│   │   │   │   ├── ProductService.java
│   │   │   │   ├── CustomerService.java
│   │   │   │   ├── OrderService.java
│   │   │   │   ├── AuthService.java
│   │   │   │   └── impl/
│   │   │   │       ├── CategoryServiceImpl.java
│   │   │   │       ├── ProductServiceImpl.java
│   │   │   │       ├── CustomerServiceImpl.java
│   │   │   │       ├── OrderServiceImpl.java
│   │   │   │       └── AuthServiceImpl.java
│   │   │   ├── controller/
│   │   │   │   ├── CategoryController.java     (@RestController, annotated)
│   │   │   │   ├── ProductController.java      (@RestController, annotated)
│   │   │   │   ├── OrderController.java        (@RestController, annotated + SSE)
│   │   │   │   └── AuthController.java
│   │   │   ├── router/
│   │   │   │   ├── CustomerRouter.java         (RouterFunction, functional style)
│   │   │   │   └── CustomerHandler.java
│   │   │   ├── exception/
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── BusinessRuleException.java
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   └── GlobalExceptionHandler.java (WebExceptionHandler)
│   │   │   ├── security/
│   │   │   │   ├── JwtService.java
│   │   │   │   ├── JwtAuthWebFilter.java       (WebFilter)
│   │   │   │   └── ReactiveUserDetailsServiceImpl.java
│   │   │   ├── aspect/
│   │   │   │   ├── LoggingAspect.java
│   │   │   │   └── ExecutionTimeAspect.java
│   │   │   └── scheduler/
│   │   │       └── StockReportScheduler.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-test.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   │           ├── V1__init_schema.sql
│   │           └── V2__init_users_and_roles.sql
│   └── test/
│       └── java/edgareldy/springwebfluxtutorial/
│           ├── controller/ (WebTestClient)
│           ├── router/ (WebTestClient on RouterFunction)
│           ├── service/ (StepVerifier + Mockito)
│           └── repository/ (@DataR2dbcTest / Testcontainers)
├── docker-compose.yml
├── Dockerfile
├── .github/workflows/ci.yml
├── pom.xml
└── README.md
```

## Standard response format

Same principle as `spring-boot-tutorial`, adapted to reactive types: controllers and handlers return `Mono<ApiResponse<T>>` (single resource) or `Mono<ApiResponse<PageResponse<T>>>` (paginated list). Continuous stream endpoints (SSE) return a raw, unwrapped `Flux<T>`, since the `text/event-stream` protocol handles event framing itself.

```java
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, Instant.now());
    }
}
```

- Reactive pagination: since R2DBC does not natively support `Pageable` on repositories, `PageResponse<T>` is built manually in the service via `Mono.zip` between the paginated `Flux<T>` (`LIMIT`/`OFFSET`) and a total count `Mono<Long>` (`COUNT(*)`).
- `GlobalExceptionHandler` implements `WebExceptionHandler` (or `@ControllerAdvice` with `ResponseStatusException`) to stay non-blocking, and returns a `Mono<ApiResponse<Void>>`.

## Spring AOP in a reactive context

A point of attention specific to WebFlux: a classic `@Around` advice that logs before/after a method call fires **before the returned `Mono`/`Flux` is actually subscribed to**, which skews the logging (the "after" part runs before the data is even emitted).

- `LoggingAspect`: intercepts the call, retrieves the returned `Mono`/`Flux`, and attaches logging via `.doOnNext()`, `.doOnError()`, `.doFinally()` instead of running code right after the method call
- `ExecutionTimeAspect`: measures the real execution time by subscribing at the end of the stream (`.doFinally(signal -> ...)`), not at the moment of the method call
- Serves as a teaching case to illustrate why "naive" AOP does not work as-is with reactive types

## feature/core-architecture

Technical foundation shared by the whole project, to be merged first into `develop`.

### Tasks

- [ ] Initialize the project via Spring Initializr (Maven, Java 17, Spring Boot 3.5.16, WebFlux)
- [ ] Dependencies: `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `spring-boot-starter-security`, `spring-boot-starter-aop`, `r2dbc-postgresql`, `flyway-core`, `flyway-database-postgresql`, `postgresql` (JDBC driver, Flyway only), `lombok`, `mapstruct` + `mapstruct-processor`, `springdoc-openapi-starter-webflux-ui`, `jjwt-api`/`jjwt-impl`/`jjwt-jackson`
- [ ] Test dependencies: `spring-boot-starter-test` (includes `reactor-test` for `StepVerifier`), `spring-security-test`, `testcontainers` (junit-jupiter, postgresql, r2dbc)
- [ ] Create the package tree shown above
- [ ] `R2dbcConfig`: `ConnectionFactory`, `R2dbcEntityTemplate`, optional `ReactiveAuditing`
- [ ] `application.yml` / `application-dev.yml`: R2DBC URL (`r2dbc:postgresql://...`) **and** a separate JDBC URL for Flyway (`spring.flyway.url`)
- [ ] `application-test.yml`: Testcontainers datasource (R2DBC + JDBC for Flyway)
- [ ] `application-prod.yml`: configuration via environment variables
- [ ] Flyway script `V1__init_schema.sql` (categories, products, customers, orders tables)
- [ ] Non-blocking `GlobalExceptionHandler` (`ResourceNotFoundException` → 404, validation errors → 400, `BusinessRuleException` → 422, generic → 500)
- [ ] Generic `ApiResponse<T>` and `PageResponse<T>` DTOs (`dto/common/`)
- [ ] `LoggingAspect` and `ExecutionTimeAspect` adapted to reactive types (`aspect/`)
- [ ] `OpenApiConfig` (springdoc WebFlux)
- [ ] Actuator (`health`, `info`, `metrics`)
- [ ] `WebClientConfig`: reusable `WebClient.Builder` bean, with configured timeouts
- [ ] `SchedulingConfig` (`@EnableScheduling`)
- [ ] `CorsConfig`
- [ ] `Dockerfile` + `docker-compose.yml` (app + PostgreSQL)
- [ ] `.github/workflows/ci.yml`

### Configuration notes

- **Spring Boot version decided upfront (same fallback already hit on `spring-boot-tutorial`)**:
  this project targets Spring Boot 4.1.x / Spring Framework 7 on paper, but
  `springdoc-openapi-starter-webflux-ui` breaks on Spring Boot 4 (confirmed via
  `springdoc/springdoc-openapi#3196`: `SwaggerConfig` looks up
  `org.springframework.boot.autoconfigure.web.reactive.WebFluxProperties`, a class relocated
  to `org.springframework.boot.webflux.autoconfigure.WebFluxProperties` in Spring Boot 4).
  Decision made upfront this time instead of discovering the blocker mid-implementation:
  **Spring Boot 3.5.16 (Spring Framework 6)**, the same version already validated on
  `spring-boot-tutorial`.
- `r2dbc-postgresql` and `spring-boot-starter-data-r2dbc` are confirmed available for
  3.5.16 (stable R2DBC support since Boot 3.0); the generated `pom.xml` builds and compiles.

## feature/products

**Annotated controller** style (`@RestController`), to build on the habits from `spring-boot-tutorial`.

### Endpoints

| Method | URL | Return | Description |
|---|---|---|---|
| GET | `/api/v1/categories` | `Mono<ApiResponse<PageResponse<CategoryResponse>>>` | Paginated list |
| GET | `/api/v1/categories/{id}` | `Mono<ApiResponse<CategoryResponse>>` | Detail |
| POST | `/api/v1/categories` | `Mono<ApiResponse<CategoryResponse>>` | Create |
| PUT | `/api/v1/categories/{id}` | `Mono<ApiResponse<CategoryResponse>>` | Update |
| DELETE | `/api/v1/categories/{id}` | `Mono<ApiResponse<Void>>` | Delete |
| GET | `/api/v1/products` | `Mono<ApiResponse<PageResponse<ProductResponse>>>` | Paginated list, filterable by `categoryId` |
| GET | `/api/v1/products/{id}` | `Mono<ApiResponse<ProductResponse>>` | Detail (resolves the category via `flatMap`) |
| POST | `/api/v1/products` | `Mono<ApiResponse<ProductResponse>>` | Create |
| PUT | `/api/v1/products/{id}` | `Mono<ApiResponse<ProductResponse>>` | Update |
| DELETE | `/api/v1/products/{id}` | `Mono<ApiResponse<Void>>` | Delete |

### Tasks

- [ ] R2DBC entities `Category`, `Product` (`@Table`, `@Id`, `@Column`, no automatic relation)
- [ ] `CategoryRepository`, `ProductRepository` (`ReactiveCrudRepository`/`R2dbcRepository`), derived queries (`findByCategoryId`) and a custom `@Query`
- [ ] DTOs and MapStruct mappers
- [ ] `CategoryService`/`ProductService` interfaces + implementations: resolving the `Product → Category` relation via `flatMap`/`zipWith`
- [ ] Business rule: deleting a category still linked to products is forbidden (checked via `Flux.hasElements`)
- [ ] `CategoryController`, `ProductController`
- [ ] Tests: `@DataR2dbcTest` (repository), `StepVerifier` (service), `WebTestClient` (controller)

## feature/customers

**Functional endpoints** style (`RouterFunction`/`HandlerFunction`), to illustrate the alternative to annotated controllers.

### Endpoints

| Method | URL | Description |
|---|---|---|
| GET | `/api/v1/customers` | Paginated list, search by name (`?search=`) |
| GET | `/api/v1/customers/{id}` | Detail |
| POST | `/api/v1/customers` | Create |
| PUT | `/api/v1/customers/{id}` | Update |
| DELETE | `/api/v1/customers/{id}` | Delete |

### Tasks

- [ ] R2DBC entity `Customer`
- [ ] `CustomerRepository`
- [ ] DTOs `CustomerRequest`/`CustomerResponse` with validation
- [ ] `CustomerService` interface + implementation, email uniqueness check
- [ ] `CustomerHandler` (`router/CustomerHandler.java`): one method per action (`findAll`, `findById`, `create`, `update`, `delete`), each returning a `Mono<ServerResponse>`
- [ ] Manual validation in the handler via an injected `Validator` (no automatic `@Valid` in functional style)
- [ ] `CustomerRouter` (`router/CustomerRouter.java`): `RouterFunction<ServerResponse>` bean declaring routes with `RequestPredicates`
- [ ] Tests via `WebTestClient` on the functional routes

## feature/orders

Classic CRUD **and** real-time streaming via Server-Sent Events.

### Endpoints

| Method | URL | Return | Description |
|---|---|---|---|
| GET | `/api/v1/orders` | `Mono<ApiResponse<PageResponse<OrderResponse>>>` | Paginated list |
| GET | `/api/v1/orders/{id}` | `Mono<ApiResponse<OrderResponse>>` | Detail |
| POST | `/api/v1/orders` | `Mono<ApiResponse<OrderResponse>>` | Create (computes `total`) |
| PUT | `/api/v1/orders/{id}` | `Mono<ApiResponse<OrderResponse>>` | Update |
| DELETE | `/api/v1/orders/{id}` | `Mono<ApiResponse<Void>>` | Delete |
| GET | `/api/v1/orders/stream` | `Flux<OrderStatusEvent>` (`text/event-stream`) | Real-time stream of new orders/status changes |

### Tasks

- [ ] R2DBC entity `Order`
- [ ] `OrderRepository` with manual join `@Query`s (`SELECT o.*, c.first_name, p.product_name FROM orders o JOIN ...`) mapped to a dedicated projection
- [ ] DTOs `OrderRequest`/`OrderResponse`/`OrderStatusEvent`
- [ ] `OrderService` interface + implementation: resolves `Customer` and `Product` via `Mono.zip`, computes `total`
- [ ] `Sinks.Many<OrderStatusEvent>` (shared bean): every order creation/update publishes an event into this sink
- [ ] `OrderController`: `/api/v1/orders/stream` endpoint exposing `sink.asFlux()` as `MediaType.TEXT_EVENT_STREAM_VALUE`
- [ ] `WebClient` demonstration: call to a fictitious external service (e.g. address verification or exchange rate) when creating an order, with timeout/error handling (`onErrorResume`)
- [ ] `StockReportScheduler` (`scheduler/StockReportScheduler.java`): `@Scheduled` job that periodically aggregates the day's orders (log or reporting table)
- [ ] Unit tests (total computation, aggregation), SSE stream test with `WebTestClient.get().accept(TEXT_EVENT_STREAM)`

## feature/auth

### Endpoints

| Method | URL | Description | Access |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Sign up | Public |
| POST | `/api/v1/auth/login` | Sign in, returns a JWT | Public |
| GET | `/api/v1/auth/me` | Current user profile | Authenticated |

### Authorization rules

| Resource | GET | POST/PUT/DELETE |
|---|---|---|
| categories, products | Public | ADMIN |
| customers, orders | Authenticated | ADMIN |
| orders/stream | Authenticated | - |

### Tasks

- [ ] `AppUser`, `Role`, `AppUserRepository` (R2DBC)
- [ ] `JwtService` (generation, validation, claims extraction)
- [ ] `JwtAuthWebFilter` implementing `WebFilter` (reactive equivalent of `OncePerRequestFilter`)
- [ ] `ReactiveUserDetailsServiceImpl` implementing `ReactiveUserDetailsService`
- [ ] `SecurityConfig`: `SecurityWebFilterChain` bean, BCrypt `PasswordEncoder`, stateless session, per-route authorization rules
- [ ] `AuthService` interface + implementation, `AuthController`
- [ ] Reactive 401/403 error handling (`ServerAuthenticationEntryPoint`, `ServerAccessDeniedHandler`)
- [ ] Swagger UI with an "Authorize" button (Bearer JWT)
- [ ] Integration tests with `spring-security-test` (`WebTestClient` + `mutateWith(mockUser())`)

## Order of work

1. `feature/core-architecture` → Pull Request to `develop`
2. `feature/products` (depends on `core-architecture`) → Pull Request to `develop`
3. `feature/customers` (depends on `core-architecture`) → Pull Request to `develop`
4. `feature/orders` (depends on `products` and `customers`) → Pull Request to `develop`
5. `feature/auth` (secures everything) → Pull Request to `develop`
6. `develop` → `master` once everything is tested and validated

## Code conventions

- Root package: `edgareldy.springwebfluxtutorial`
- DTOs: Java `record`
- **No `.block()` anywhere in application code** (controllers, handlers, services): the whole chain must stay reactive end to end; `.block()` is only tolerated in tests or startup scripts
- **Contract/implementation services**: interface at the root of `service/`, implementation in `service/impl/`
- Every controller/handler returns an `ApiResponse<T>` wrapped in a `Mono`, except SSE streams
- Any service method that writes to the database is annotated `@Transactional` (the reactive transaction manager `R2dbcTransactionManager` must be configured in `feature/core-architecture`)
- Errors in the reactive chain are handled via `onErrorResume`/`onErrorMap`, never via `try/catch` around a blocking call

## Concepts covered

- Reactive programming with Project Reactor (`Mono`, `Flux`, operators `map`, `flatMap`, `zip`, `switchIfEmpty`, `onErrorResume`)
- Spring Data R2DBC: entities, reactive repositories, `@Query`, manual pagination
- Two WebFlux API styles: annotated controllers and functional endpoints (`RouterFunction`)
- DTOs and mapping (MapStruct)
- Reactive validation (automatic and manual)
- Non-blocking centralized exception handling
- Reactive transactions (`R2dbcTransactionManager`, `@Transactional`)
- Schema migrations (Flyway, separate JDBC connection)
- Reactive API documentation (OpenAPI / Swagger UI)
- Observability (Actuator)
- Reactive security (Spring Security WebFlux, JWT, `WebFilter`)
- Server-Sent Events (`Flux` as `text/event-stream`, `Sinks.Many`)
- Reactive HTTP client (`WebClient`, timeout and error handling)
- Aspect-oriented programming adapted to reactive types (Spring AOP + `doOnNext`/`doFinally`)
- Scheduled tasks (`@Scheduled`)
- Reactive tests (`StepVerifier`, `WebTestClient`, Testcontainers)
- Containerization (Docker, docker-compose)
- Continuous integration (GitHub Actions)

## How to follow this tutorial

1. Clone the repository and check out `develop`
2. Create/checkout the `feature/core-architecture` branch and follow its task checklist
3. Continue with `feature/products`, `feature/customers`, `feature/orders`, `feature/auth` in that order
4. Open a Pull Request to `develop` at the end of each branch
5. Run the project with `docker-compose up`, then open Swagger UI at `http://localhost:8080/webjars/swagger-ui/index.html`
