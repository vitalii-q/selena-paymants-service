# Selena Payments Service

`payments-service` owns the payment lifecycle for Selena bookings. It is a
separate Spring Boot microservice and will own its own database.

## Technology and runtime

- Java 17, Spring Boot 3.4.3 and Maven
- Spring Web, Validation, JPA, Liquibase, Actuator and Prometheus
- PostgreSQL as the service-owned database; Testcontainers PostgreSQL for integration tests
- Development profile: port `9069`; production profile: port `9087`
- The development PostgreSQL container is mapped to host port `9269`.

Start locally with a configured PostgreSQL datasource:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Start the isolated development containers:

```bash
docker build --no-cache -f Dockerfile.dev -t selena-payments-service:latest .
docker run -d --name payments-db -p 9269:5432 \
  --env-file .env --network selena-dev_app_network \
  -v payments-postgres-data:/var/lib/postgresql/data postgres:16
docker run -d --name payments-service -p 9069:9069 \
  --env-file .env --network selena-dev_app_network \
  -v $(pwd):/app -v payments-maven-cache:/root/.m2 \
  selena-payments-service:latest
```

The API is available on `http://localhost:9069`; PostgreSQL is exposed locally on
`localhost:9269`.

When started through `Dockerfile.dev`, the entrypoint waits for PostgreSQL,
creates the service database and user if necessary, validates the Liquibase
changelog and then applies pending migrations before starting Spring Boot.
It requires `PAYMENTS_POSTGRES_HOST`, `PAYMENTS_POSTGRES_DB_NAME`,
`POSTGRES_PASSWORD`, `SPRING_DATASOURCE_USERNAME` and
`SPRING_DATASOURCE_PASSWORD`; `PAYMENTS_POSTGRES_PORT_INNER` defaults to
`3306` and `ROOT_USER` defaults to `root`.

## Scope and ownership

- The service owns payment records, payment status, provider transaction IDs,
  idempotency keys and refund information.
- `bookings-service` remains the source of truth for booking details, booking
  status and the booking price.
- A payment stores `bookingId` (currently the `Long` ID used by
  `bookings-service`) and `userId` as reference values only. There are no SQL
  foreign keys to another service's database.
- Cross-service consistency is maintained through API/event contracts, never
  through a shared database or distributed transaction.

## Initial payment contract

### Currency and amount

- Version 1 supports `EUR` only, represented by the ISO 4217 currency code.
- Amount is a positive decimal value with two fractional digits at most.
- The requested amount is immutable once a payment is created.
- Only a trusted internal caller (`bookings-service`) may initiate a payment.
  A public client must not be able to choose an arbitrary booking amount.
- Before the integration is enabled, `bookings-service` must provide a
  calculated, non-zero booking price; its current zero-price placeholder is
  not eligible for payment creation.

### Lifecycle

| Status | Meaning | Allowed next statuses |
| --- | --- | --- |
| `PENDING` | Payment was created and awaits provider processing. | `SUCCEEDED`, `FAILED`, `CANCELLED` |
| `SUCCEEDED` | Payment was accepted by the provider. | `REFUNDED` |
| `FAILED` | Provider rejected or could not process the payment. | none |
| `CANCELLED` | A pending payment was cancelled before success. | none |
| `REFUNDED` | A successful payment was fully refunded. | none |

Version 1 supports one full refund only. Partial refunds, retries of failed
payments and payment-method changes are explicitly out of scope.

### Idempotency

Creating a payment requires an `Idempotency-Key` request header containing a
UUID. The key is unique per user.

- Repeating the same key with the same `bookingId`, amount and currency returns
  the original payment result; it must not call the provider again.
- Reusing a key with different request data returns `409 Conflict`.
- The database will enforce uniqueness for `(user_id, idempotency_key)`.

### Booking integration contract

- A successful payment produces `PaymentSucceeded` with `paymentId`,
  `bookingId`, `userId`, amount, currency and occurrence time.
- A failed or cancelled payment produces `PaymentFailed` or `PaymentCancelled`
  with the same correlation fields and a safe failure code.
- `bookings-service` consumes these facts and changes only its own booking
  status. It must process each event idempotently.
- The initial implementation may use a stub provider and a temporary internal
  HTTP callback; its event payload and state transitions remain the stable
  contract for a later message broker/outbox implementation.

## Security boundary

The service must never store or log card numbers, CVV values or other raw card
data. The stub provider will receive only a non-sensitive test payment token.
