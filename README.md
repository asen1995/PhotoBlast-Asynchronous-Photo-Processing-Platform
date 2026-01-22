# PhotoBlast

**Asynchronous Photo Processing Platform**

PhotoBlast is a scalable backend system designed to handle asynchronous processing of user-uploaded photos. Users upload images via a REST API, which queues processing tasks such as resizing, watermarking, and thumbnail generation. These tasks are distributed reliably across multiple worker services using RabbitMQ, ensuring fault tolerance, scalability, and smooth user experience without blocking requests.

## Technology Stack

| Technology | Purpose |
|------------|---------|
| Java 21 | Runtime |
| Spring Boot 4 | Application framework |
| Spring AMQP | RabbitMQ integration |
| RabbitMQ | Message broker |
| Docker Compose | Local orchestration |

## Getting Started

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### Running Locally

1. **Start RabbitMQ**
   ```bash
   docker-compose up -d
   ```

2. **Access RabbitMQ Management UI**
   - URL: http://localhost:15672
   - Username: `photoblast`
   - Password: `photoblast123`

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   REST API      │     │    RabbitMQ     │     │     Worker      │
│   (Producer)    │────>│   Message Queue │────>│   (Consumer)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       │
        v                       v                       v
   Upload Photo          job.photo.process        Process Tasks
   Validate & Store      Dead Letter Queue        Store Results
   Publish Job           Retry Mechanism          Notify User
```

## Core Components

### Photo Upload API (Producer Service)

- REST endpoint to accept photo uploads
- Validates and stores original photos (local disk or cloud storage)
- Publishes a job message to RabbitMQ to process the photo asynchronously

### RabbitMQ Messaging Layer

| Queue | Purpose |
|-------|---------|
| `job.photo.process` | Main queue for photo processing tasks |
| `job.photo.process.dlq` | Dead-letter queue for failed jobs |

Features:
- Retry mechanism using delayed messages and TTL
- Message acknowledgment on successful processing

### Photo Processing Worker (Consumer Service)

- Multiple instances can consume from the photo processing queue
- Performs tasks: **resizing**, **watermarking**, **thumbnail creation**
- Acknowledges messages upon success; failed jobs are retried or dead-lettered

### Storage & Result Delivery

- Stores processed photos in a separate location
- Optional: Notifies users when processing completes (email/push notifications)

## Project Structure

```
src/main/java/com/photoblast/
├── config/
│   └── RabbitMQConfig.java       # Queue, exchange, and binding setup
├── model/
│   └── PhotoProcessingJob.java   # Job message model
└── service/
    ├── PhotoJobProducer.java     # Publishes jobs to RabbitMQ
    └── PhotoJobConsumer.java     # Processes jobs from queue
```

## Configuration

Configuration is managed in `src/main/resources/application.yml`:

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: photoblast
    password: photoblast123
    virtual-host: photoblast
```

