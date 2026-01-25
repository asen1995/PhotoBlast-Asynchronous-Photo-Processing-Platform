# PhotoBlast

**Asynchronous Photo Processing Platform**

PhotoBlast is a scalable photo processing platform with a React frontend and Spring Boot backend. Users upload images through a web interface, which queues processing tasks such as resizing, watermarking, and thumbnail generation. Tasks are distributed reliably using RabbitMQ, ensuring fault tolerance and scalability.

## Technology Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| Frontend | React + Vite | Web interface for photo uploads |
| Backend | Java 21 + Spring Boot | REST API and job processing |
| Messaging | RabbitMQ | Async task queue |
| Containerization | Docker Compose | Local orchestration |

## Getting Started

### Prerequisites

- Docker & Docker Compose

For local development without Docker:
- Java 21+
- Maven 3.9+
- Node.js 20+

### Running with Docker Compose (Recommended)

Start all services with a single command:

```bash
docker-compose up -d
```

This starts three services in order:

1. **RabbitMQ** - Message broker (waits for health check)
2. **Backend** - Spring Boot API (waits for RabbitMQ)
3. **Frontend** - React app via Nginx (waits for Backend)

### Accessing the Application

| Service | URL | Description |
|---------|-----|-------------|
| Frontend | http://localhost:3000 | Photo upload interface |
| Backend API | http://localhost:8080/api | REST API |
| RabbitMQ UI | http://localhost:15672 | Message queue management |

RabbitMQ credentials:
- Username: `photoblast`
- Password: `photoblast123`

### Stopping the Application

```bash
docker-compose down
```

To also remove volumes (uploaded photos, RabbitMQ data):

```bash
docker-compose down -v
```

### Running Locally (Development)

1. **Start RabbitMQ**
   ```bash
   docker-compose up -d rabbitmq
   ```

2. **Start the Backend**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Start the Frontend**
   ```bash
   cd front-end
   npm install
   npm run dev
   ```

   Frontend dev server runs at http://localhost:5173 with hot reload.

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    Frontend     │     │     Backend     │     │    RabbitMQ     │
│   (React/Vite)  │────>│  (Spring Boot)  │────>│  Message Queue  │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       │
        v                       v                       v
   Upload UI              REST API               job.photo.process
   Task Selection         Validate & Store       Dead Letter Queue
   Progress Display       Publish Job            Retry Mechanism
                                │
                                v
                        ┌─────────────────┐
                        │     Worker      │
                        │   (Consumer)    │
                        └─────────────────┘
                                │
                                v
                          Process Tasks
                          Store Results
```

## Features

### Frontend
- Drag & drop image upload
- Click to browse files
- Image preview before upload
- Selectable processing tasks:
  - **Resize** - Scale to 1920x1080
  - **Thumbnail** - Generate 200x200 thumbnail
  - **Watermark** - Apply watermark overlay
- Upload progress and status feedback

### Backend
- REST API for photo uploads
- Image validation (type checking)
- Async job processing via RabbitMQ
- Multiple processing tasks per upload

## API Endpoints

### Upload Photo
```
POST /api/photos/upload
Content-Type: multipart/form-data

Parameters:
- file: Image file (required)
- tasks: Processing tasks (optional, default: RESIZE,THUMBNAIL)
         Values: RESIZE, THUMBNAIL, WATERMARK

Response:
{
  "success": true,
  "message": "Photo uploaded successfully",
  "jobId": "uuid",
  "photoId": "uuid",
  "tasks": ["RESIZE", "THUMBNAIL"]
}
```

### Health Check
```
GET /api/photos/health

Response: OK
```

## Project Structure

```
PhotoBlast/
├── docker-compose.yml          # Full stack orchestration
├── Dockerfile                  # Backend container
├── pom.xml                     # Maven configuration
├── src/main/java/com/photoblast/
│   ├── config/                 # RabbitMQ and app configuration
│   ├── controller/             # REST controllers
│   ├── dto/                    # Data transfer objects
│   ├── model/                  # Domain models
│   ├── service/                # Business logic
│   └── util/                   # Utility classes
└── front-end/
    ├── Dockerfile              # Frontend container
    ├── nginx.conf              # Production server config
    ├── src/
    │   ├── components/         # React components
    │   ├── App.jsx             # Main app component
    │   └── main.jsx            # Entry point
    └── vite.config.js          # Vite configuration
```

## Configuration

### Backend (application.yml)

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: photoblast
    password: photoblast123
    virtual-host: photoblast

photoblast:
  image:
    resize:
      width: 1920
      height: 1080
    thumbnail:
      width: 200
      height: 200
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| RABBITMQ_USER | photoblast | RabbitMQ username |
| RABBITMQ_PASS | photoblast123 | RabbitMQ password |
| RABBITMQ_VHOST | photoblast | RabbitMQ virtual host |

## Docker Compose Services

| Service | Container Name | Ports | Health Check |
|---------|----------------|-------|--------------|
| rabbitmq | photoblast-rabbitmq | 5672, 15672 | Port connectivity |
| backend | photoblast-backend | 8080 | /api/photos/health |
| frontend | photoblast-frontend | 3000 | Depends on backend |

### Volumes

| Volume | Purpose |
|--------|---------|
| rabbitmq_data | RabbitMQ persistent data |
| uploads_data | Original uploaded photos |
| processed_data | Processed photos |
| thumbnails_data | Generated thumbnails |
