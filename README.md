# Load Balancing Module

**Production-grade AWS Infrastructure Control Plane**

This module provides live infrastructure load balancing capabilities operating on real AWS environments across multiple accounts and regions.

## Architecture

- **Hybrid Control Plane**: Combines AWS-native primitives (ALB, ASG) with intelligent control logic
- **AI/ML Advisory**: Optional LSTM load prediction and Isolation Forest anomaly detection
- **Multi-Account**: STS AssumeRole with platform-generated External IDs
- **Multi-Region**: Route 53 for global traffic management

## Structure

```
load-balancing/
├── backend/           # Spring Boot backend service
├── frontend/          # React frontend module
└── ml-service/        # Python AI/ML advisory service (optional)
```

## Quick Start

### Backend
```bash
cd backend
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

## Documentation

See [LOAD_BALANCING_ARCHITECTURE_AND_PLAN.md](../LOAD_BALANCING_ARCHITECTURE_AND_PLAN.md) for full architecture documentation.
