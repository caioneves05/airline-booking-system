# Plane Ticket Booking System - Architecture

## Microservices Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              API Gateway                                     │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
        ┌───────────────┬───────────┼───────────┬───────────────┐
        ▼               ▼           ▼           ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│   Flight     │ │   Booking    │ │   Payment    │ │   Customer   │ │ Notification │
│   Service    │ │   Service    │ │   Service    │ │   Service    │ │   Service    │
└──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘
        │               │                │               │               │
        ▼               ▼                ▼               ▼               ▼
   [PostgreSQL]    [PostgreSQL]     [PostgreSQL]   [PostgreSQL]      [Redis]
                        │
                        ▼
                    [Redis]
                (Seat Locking)
```

---

## Services Breakdown

### 1. Flight Service
**Responsibility**: Manage flights, routes, aircraft, and seat inventory

| Feature | Description |
|---------|-------------|
| Flight CRUD | Create, update, cancel flights |
| Seat Map | Available seats per flight |
| Pricing | Dynamic pricing based on demand |
| Search | Find flights by origin, destination, date |

**Database**: PostgreSQL (flights, aircraft, routes, seats)

**Caching**: Redis for flight search results

---

### 2. Booking Service (Core)
**Responsibility**: Orchestrate the booking flow

| Feature | Description |
|---------|-------------|
| Seat Reservation | Temporary lock (TTL-based) |
| Booking Creation | Create booking record |
| Saga Orchestration | Coordinate payment + confirmation |
| Cancellation | Handle refunds and seat release |

**Database**: PostgreSQL (bookings, passengers, booking_status)

**Redis**: Distributed locks for seat reservation (prevent double-booking)

**Resilience**:
- Circuit breaker for Payment Service calls
- Saga pattern with compensating transactions
- Idempotency keys for booking requests

---

### 3. Payment Service
**Responsibility**: Process payments and refunds

| Feature | Description |
|---------|-------------|
| Payment Processing | Integrate with payment gateway |
| Refund Handling | Process cancellation refunds |
| Payment Status | Track payment lifecycle |

**Database**: PostgreSQL (payments, transactions, refunds)

**Resilience**:
- Retry with exponential backoff for gateway timeouts
- Idempotency to prevent duplicate charges
- Circuit breaker for external gateway

---

### 4. Customer Service
**Responsibility**: User management and authentication

| Feature | Description |
|---------|-------------|
| Registration | Create customer accounts |
| Authentication | JWT token management |
| Profile | Manage passenger details |
| Booking History | View past bookings |

**Database**: PostgreSQL (customers, passenger_profiles)

---

### 5. Notification Service
**Responsibility**: Send all system notifications

| Feature | Description |
|---------|-------------|
| Email | Booking confirmation, e-ticket |
| SMS | Flight reminders, gate changes |
| Push | Mobile app notifications |

**Database**: Redis (notification queue, delivery status)

---

## Event Flow with SNS/SQS

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           SNS Topics                                         │
├─────────────────┬─────────────────┬─────────────────┬───────────────────────┤
│ booking-events  │ payment-events  │ flight-events   │ notification-requests │
└────────┬────────┴────────┬────────┴────────┬────────┴───────────┬───────────┘
         │                 │                 │                     │
         ▼                 ▼                 ▼                     ▼
    ┌─────────┐       ┌─────────┐       ┌─────────┐          ┌─────────┐
    │   SQS   │       │   SQS   │       │   SQS   │          │   SQS   │
    │ Queue   │       │ Queue   │       │ Queue   │          │ Queue   │
    └────┬────┘       └────┬────┘       └────┬────┘          └────┬────┘
         │                 │                 │                     │
         ▼                 ▼                 ▼                     ▼
   Notification      Booking           Booking              Notification
    Service          Service           Service                Service
```

### Event Examples

| Event | Publisher | Subscribers |
|-------|-----------|-------------|
| `BookingCreated` | Booking Service | Notification, Flight |
| `PaymentCompleted` | Payment Service | Booking |
| `PaymentFailed` | Payment Service | Booking, Notification |
| `BookingConfirmed` | Booking Service | Notification |
| `BookingCancelled` | Booking Service | Payment, Flight, Notification |
| `FlightDelayed` | Flight Service | Notification |
| `FlightCancelled` | Flight Service | Booking, Notification |

---

## Booking Flow (Saga Pattern)

```
Customer                Booking              Flight             Payment           Notification
   │                       │                    │                   │                   │
   │──── Search Flights ───▶                    │                   │                   │
   │                       │── Get Available ──▶│                   │                   │
   │◀── Flight Options ────│◀─────────────────  │                   │                   │
   │                       │                    │                   │                   │
   │──── Select Seat ─────▶│                    │                   │                   │
   │                       │── Lock Seat ──────▶│                   │                   │
   │                       │◀─ Seat Locked ─────│                   │                   │
   │◀── Reservation OK ────│                    │                   │                   │
   │    (TTL: 10 min)      │                    │                   │                   │
   │                       │                    │                   │                   │
   │──── Confirm + Pay ───▶│                    │                   │                   │
   │                       │────────────────── Process Payment ────▶│                   │
   │                       │◀─────────────────── Payment Result ────│                   │
   │                       │                    │                   │                   │
   │                       │── Confirm Seat ───▶│  (if success)     │                   │
   │                       │                    │                   │                   │
   │                       │─────────────────── Send Confirmation ─────────────────────▶│
   │◀── Booking Complete ──│                    │                   │                   │
   │                       │                    │                   │                   │
```

### Compensation (Rollback) Scenarios

| Failure Point | Compensation Action |
|---------------|---------------------|
| Payment fails | Release seat lock |
| Seat confirm fails | Refund payment, release lock |
| Notification fails | Retry via DLQ (non-blocking) |

---

## Resilience Patterns by Service

| Service | Pattern | Purpose |
|---------|---------|---------|
| Booking | Circuit Breaker | Fail fast if Payment is down |
| Booking | Distributed Lock (Redis) | Prevent double-booking |
| Booking | Saga + Compensation | Rollback on partial failure |
| Payment | Retry + Backoff | Handle gateway timeouts |
| Payment | Idempotency Key | Prevent duplicate charges |
| Flight | Bulkhead | Isolate search from booking operations |
| All | Dead Letter Queue | Capture failed events for retry |
| All | Health Checks | Kubernetes/ECS readiness probes |

---

## Infrastructure (Terraform)

```
terraform/
├── modules/
│   ├── vpc/                  # VPC, subnets, NAT gateway
│   ├── ecs-cluster/          # ECS Fargate cluster
│   ├── ecs-service/          # Reusable service module
│   ├── rds/                  # PostgreSQL RDS
│   ├── elasticache/          # Redis cluster
│   ├── sqs/                  # SQS queues + DLQs
│   ├── sns/                  # SNS topics
│   ├── ecr/                  # Container registries
│   ├── alb/                  # Application Load Balancer
│   └── secrets/              # Secrets Manager
├── environments/
│   ├── dev/
│   │   └── main.tf
│   ├── staging/
│   │   └── main.tf
│   └── prod/
│       └── main.tf
└── backend.tf                # S3 state backend
```

---

## CI/CD Pipeline

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  Commit  │───▶│  Build   │───▶│  Test    │───▶│Push ECR  │───▶│  Deploy  │
│          │    │  (Maven) │    │  (Unit/  │    │          │    │  (ECS)   │
│          │    │          │    │   Integ) │    │          │    │          │
└──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
```

### Pipeline Stages

1. **Build**: Compile, run checkstyle/spotbugs
2. **Unit Tests**: Fast tests, mocked dependencies
3. **Integration Tests**: Testcontainers (Postgres, Redis, LocalStack for SQS/SNS)
4. **Docker Build**: Multi-stage Dockerfile
5. **Push to ECR**: Tag with commit SHA + `latest`
6. **Deploy Dev**: Automatic
7. **Deploy Staging**: Automatic
8. **Deploy Prod**: Manual approval

---

## Project Structure (per service)

```
booking-service/
├── src/main/java/com/airline/booking/
│   ├── config/           # Spring configurations
│   ├── controller/       # REST endpoints
│   ├── service/          # Business logic
│   ├── repository/       # Data access
│   ├── domain/           # Entities
│   ├── dto/              # Request/Response objects
│   ├── event/            # SNS/SQS publishers and listeners
│   ├── saga/             # Saga orchestration
│   ├── exception/        # Custom exceptions + handlers
│   └── resilience/       # Circuit breaker configs
├── src/main/resources/
│   ├── application.yml
│   └── application-{env}.yml
├── src/test/
│   ├── unit/
│   └── integration/
├── Dockerfile
└── pom.xml
```

---

## Suggested Implementation Order

1. **Customer Service** - Auth, profiles (current)
2. **Flight Service** - CRUD, search
3. **Booking Service** - Core flow without payment
4. **Payment Service** - Integration + resilience
5. **Notification Service** - Event consumers
6. **Infrastructure** - Terraform modules
7. **CI/CD** - GitHub Actions pipeline
8. **Observability** - Metrics, logs, traces
