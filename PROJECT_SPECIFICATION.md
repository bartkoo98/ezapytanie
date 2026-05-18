# eZapytanie — Project Analysis & Technical Specification

> **Document Version:** 1.0  
> **Author:** Engineering Thesis — IT Student  
> **Role:** Lead Systems Architect / Product Owner  
> **Last Updated:** 2026-05-03  
> **Status:** Active Development Guide

---

## Table of Contents

1. [Project Analysis](#1-project-analysis)
2. [Requirements](#2-requirements)
3. [Risk & Security Analysis](#3-risk--security-analysis)
4. [System Architecture](#4-system-architecture)
5. [Mermaid Diagrams](#5-mermaid-diagrams)
6. [Professional Backlog](#6-professional-backlog)
7. [Engineering Best Practices](#7-engineering-best-practices)

---

## 1. Project Analysis

### 1.1 Executive Summary

**eZapytanie** is a web application designed to digitize and formalize the procurement process for public and private institutions managing Requests for Quotations (RFQs) for contracts valued below **PLN 170,000** — the threshold under which Polish public procurement law (Prawo zamówień publicznych) does not require a full tender process.

The current state of the art is fragmented: institutions send RFQs via email, track offers in spreadsheets, and have no auditable trail. eZapytanie replaces this with a structured, transparent, document-anchored workflow.

---

### 1.2 Business Goals

| # | Goal | Success Metric |
|---|------|---------------|
| BG-1 | Eliminate email-based RFQ chaos | 100% of inquiries created within the system |
| BG-2 | Ensure offer confidentiality until deadline | Zero price leaks before bid opening (Sealed Bids) |
| BG-3 | Create an auditable procurement trail | Full audit log for every state change |
| BG-4 | Reduce administrative workload | Deadline-based auto-reveal of offers |
| BG-5 | Enable multi-contractor competition | Minimum 2 contractors invited per inquiry |

---

### 1.3 Target Users

#### Primary User — Client (Zamawiający / Authority)

An employee of a public or private institution (e.g., municipality, school, hospital, company) responsible for procurement. They:

- Create and manage RFQs
- Define deadlines, requirements, and documentation
- Invite contractors
- Review revealed offers after the deadline
- Select the winning offer and archive the process

**Technical profile:** Non-technical. Uses a web browser. Expects a simple, form-based UI.

#### Secondary User — Contractor (Wykonawca / Supplier)

A company or individual that receives invitation links and submits offers. They:

- Register and maintain a company profile
- Receive email notifications of new inquiries
- Upload offer documents and enter pricing
- Cannot see competitor offers before the deadline

**Technical profile:** Non-technical. May work on mobile. Expects a guided submission form.

#### System Role — Administrator

A system administrator (likely the thesis author / deploying institution). They:

- Manage user accounts and roles
- Access full audit logs
- Override deadlines in exceptional cases (with logged justification)
- Monitor system health

---

### 1.4 Detailed User Roles & Permissions

```
ROLE: ADMIN
  ├── All CLIENT permissions
  ├── All CONTRACTOR permissions  
  ├── View all audit logs (system-wide)
  ├── Manage users (create, deactivate, change role)
  ├── Override inquiry deadlines (with mandatory justification)
  └── Access system health dashboard

ROLE: CLIENT
  ├── Create / Edit / Cancel inquiries (own)
  ├── Invite contractors to inquiries
  ├── View inquiry status and offer COUNT (not prices) before deadline
  ├── View ALL offer details AFTER deadline
  ├── Download offer documents
  ├── Mark winning offer
  ├── Export inquiry report (PDF)
  └── View own audit history

ROLE: CONTRACTOR
  ├── View inquiries they are invited to
  ├── Submit ONE offer per inquiry (before deadline)
  ├── Edit own offer (before deadline)
  ├── Withdraw own offer (before deadline, with reason)
  ├── View own offer details at all times
  ├── View OTHER offers only AFTER deadline
  └── View own participation history
```

---

### 1.5 MVP Scope vs. Future Possibilities

#### ✅ MVP — Phase 1 (Thesis Deliverable)

- User registration, login, JWT authentication
- Role-based access control (Admin, Client, Contractor)
- Full CRUD for Inquiries (by Client)
- Contractor invitation system (by email or direct link)
- Offer submission with document upload
- **Sealed Bids:** Price/details hidden until inquiry deadline passes
- Automatic bid reveal when deadline is crossed
- Basic audit logging (who did what, when)
- Simple dashboard for each role
- Docker-based local deployment

#### 🔮 Future Possibilities — Phase 2+

| Feature | Notes |
|---------|-------|
| Email notification system | SendGrid / SMTP integration |
| PDF report generation | JasperReports or iText |
| Public inquiry board | Anonymous browsing of open RFQs |
| Contractor rating system | Post-contract feedback |
| e-Signature integration | Qualified electronic signature (KSEF-compatible) |
| Two-factor authentication | TOTP / SMS |
| Multi-language support | PL / EN |
| REST API for ERP integration | Webhook support |
| Advanced search & filtering | Elasticsearch |
| Analytics dashboard | Inquiry completion rates, avg. offers per RFQ |

---

## 2. Requirements

### 2.1 Functional Requirements

#### Authentication & Authorization (AUTH)

| ID | Requirement |
|----|-------------|
| FR-AUTH-01 | Users must register with email, password, full name, and institution name |
| FR-AUTH-02 | Passwords must be hashed using BCrypt (min cost factor 12) |
| FR-AUTH-03 | Login must return a signed JWT access token (15 min) and refresh token (7 days) |
| FR-AUTH-04 | All protected endpoints must validate the JWT on every request |
| FR-AUTH-05 | Role-based access control must be enforced at the API layer, not only UI |
| FR-AUTH-06 | Users can refresh their session without re-login using the refresh token |
| FR-AUTH-07 | Admins can deactivate accounts, immediately invalidating all active tokens |

#### Inquiry Management (INQ)

| ID | Requirement                                                                                  |
|----|----------------------------------------------------------------------------------------------|
| FR-INQ-01 | Client can create an inquiry with: title, description, deadline and category                 |
| FR-INQ-02 | Inquiry must have a lifecycle: `DRAFT → PUBLISHED → CLOSED → ARCHIVED`                       |
| FR-INQ-03 | Client can invite specific Contractors by email to a published inquiry                       |
| FR-INQ-04 | Client can view the number of submitted offers before the deadline (count only, not content) |
| FR-INQ-05 | Client can cancel an inquiry (moves to `CANCELLED` state) with a mandatory reason            |
| FR-INQ-06 | Deadline extension is allowed only by Admin and must be logged                               |
| FR-INQ-07 | The system must automatically transition `PUBLISHED → CLOSED` when the deadline passes       |

#### Offer Management (OFF)

| ID | Requirement |
|----|-------------|
| FR-OFF-01 | Invited Contractor can submit exactly one offer per inquiry |
| FR-OFF-02 | Offer must include: price (gross), currency (PLN default), validity date, and optional documents |
| FR-OFF-03 | Contractor can edit their offer while the inquiry is `PUBLISHED` and deadline has not passed |
| FR-OFF-04 | Offer price and documents are **never visible to the Client before the deadline** |
| FR-OFF-05 | After the deadline, all offer details are revealed to the Client automatically |
| FR-OFF-06 | Contractor can withdraw their offer before the deadline with a mandatory reason |
| FR-OFF-07 | Client can mark one offer as the winner; all others are automatically marked as rejected |

#### Audit & Reporting (AUD)

| ID | Requirement |
|----|-------------|
| FR-AUD-01 | Every state change on Inquiry or Offer must write an AuditLog entry |
| FR-AUD-02 | AuditLog entry must contain: actorId, actorRole, action, entityType, entityId, timestamp, ipAddress |
| FR-AUD-03 | Admins can query audit logs by entity, actor, or date range |
| FR-AUD-04 | Client can export a summary of a closed inquiry (JSON or basic HTML print view) |

---

### 2.2 Non-Functional Requirements

#### Security (NFR-SEC)

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-SEC-01 | HTTPS enforced in production (TLS 1.2+) | All traffic |
| NFR-SEC-02 | JWT secret stored in environment variable, never in source code | 100% |
| NFR-SEC-03 | Input validation on all API endpoints (Jakarta Bean Validation) | 100% of endpoints |
| NFR-SEC-04 | Protection against common OWASP Top 10 vulnerabilities | Spring Security defaults + custom rules |
| NFR-SEC-05 | Rate limiting on auth endpoints (max 10 failed logins / 15 min per IP) | Auth routes |
| NFR-SEC-06 | File upload validation (type whitelist: PDF, DOCX, XLSX, PNG, JPG; max 10MB) | All uploads |
| NFR-SEC-07 | Sealed bids never returned in API response before deadline (enforced server-side) | 100% |

#### Auditability (NFR-AUD)

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-AUD-01 | Audit logs are append-only (no update/delete operations allowed) | 100% |
| NFR-AUD-02 | All document versions retained for at least 5 years (configurable) | All documents |
| NFR-AUD-03 | Every database write to Inquiry or Offer triggers an audit log write | 100% |
| NFR-AUD-04 | System clock used for all timestamps; timezone stored as UTC | All timestamps |

#### Performance (NFR-PERF)

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-PERF-01 | API response time for list operations | < 500ms (p95) |
| NFR-PERF-02 | File upload endpoint | Supports up to 10MB with progress |
| NFR-PERF-03 | Concurrent users (MVP) | 50 simultaneous users |

#### Maintainability (NFR-MAINT)

| ID | Requirement |
|----|-------------|
| NFR-MAINT-01 | Backend test coverage > 70% (unit + integration) |
| NFR-MAINT-02 | All public service methods must have Javadoc |
| NFR-MAINT-03 | API documented with OpenAPI 3.0 (Springdoc) |
| NFR-MAINT-04 | Docker Compose must bring up the full stack with one command |

---

## 3. Risk & Security Analysis

### 3.1 Risk Register

| ID | Risk | Likelihood | Impact | Mitigation |
|----|------|-----------|--------|------------|
| R-01 | Sealed bid leak (Client reads offer before deadline) | Low | Critical | Server-side enforcement; field-level response filtering |
| R-02 | JWT token theft | Medium | High | Short expiry (15min); HTTPS only; HttpOnly refresh cookie |
| R-03 | Malicious file upload | Medium | High | MIME type + magic byte validation; sandboxed storage |
| R-04 | Audit log tampering | Low | High | Append-only collection; admin read-only access to logs |
| R-05 | Deadline manipulation | Low | High | Only Admin can modify; all changes logged with justification |
| R-06 | Single developer bus factor | High | Medium | Comprehensive docs; clean architecture; README-first dev |
| R-07 | MongoDB injection | Medium | High | Spring Data abstraction; never raw query strings |
| R-08 | Scope creep delaying thesis | High | Medium | Strict MVP boundary; backlog items labeled PHASE_2 |

---

### 3.2 Sealed Bids — Security Design

The **Sealed Bid** mechanism is the most critical security feature. Its integrity must be enforced at the **API layer**, never relying on the frontend.

**Enforcement Strategy:**

```
1. When a Client calls GET /api/inquiries/{id}/offers (before deadline):
   - Backend checks: inquiry.deadline > now()
   - If TRUE: Response includes offers but with price=null, documents=[]
   - Only offer count and submittedAt timestamp are visible
   
2. When deadline passes:
   - Scheduled job (@Scheduled every 60s) checks all PUBLISHED inquiries
   - If inquiry.deadline <= now(): status → CLOSED
   - AuditLog entry written: actor=SYSTEM, action=AUTO_CLOSE
   
3. When Client calls GET /api/inquiries/{id}/offers (after deadline):
   - Backend checks: inquiry.status == CLOSED || ARCHIVED
   - Full offer data returned
   
4. Critical invariant: The price field is NEVER returned in API responses
   for PUBLISHED inquiries, regardless of who asks. This is enforced in
   the OfferResponseMapper, not in the controller.
```

**MongoDB field-level control (OfferResponseMapper):**

```java
// This mapper is the single source of truth for offer visibility
public OfferResponseDto toDto(Offer offer, Inquiry inquiry) {
    boolean bidsRevealed = inquiry.getStatus() != InquiryStatus.PUBLISHED;
    return OfferResponseDto.builder()
        .id(offer.getId())
        .contractorName(offer.getContractorName())
        .submittedAt(offer.getSubmittedAt())
        // SEALED: price only visible after deadline
        .price(bidsRevealed ? offer.getPrice() : null)
        .currency(bidsRevealed ? offer.getCurrency() : null)
        // SEALED: documents only visible after deadline
        .documents(bidsRevealed ? offer.getDocuments() : Collections.emptyList())
        .status(offer.getStatus())
        .build();
}
```

---

### 3.3 Document Security

- Files are stored in a **non-web-accessible directory** (inside Docker volume, not served statically)
- File download requires valid JWT; ownership is verified before serving
- File names are **sanitized and replaced with UUID-based names** on upload
- Original file names stored in MongoDB metadata only
- A Contractor can only download their own offer's documents before the deadline; all documents accessible after deadline (to Client and Admin)

---

## 4. System Architecture

### 4.1 Component Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Docker Network                            │
│                                                                 │
│   ┌─────────────┐      ┌──────────────────┐    ┌─────────────┐ │
│   │   React UI  │ ───► │  Spring Boot API  │───►│   MongoDB   │ │
│   │  (Nginx:80) │      │   (Port 8080)     │    │  (Port 27017)│ │
│   └─────────────┘      └──────────────────┘    └─────────────┘ │
│                                │                                 │
│                         ┌──────▼──────┐                         │
│                         │  File Store │                          │
│                         │ (Docker Vol)│                          │
│                         └─────────────┘                         │
└─────────────────────────────────────────────────────────────────┘
```

---

### 4.2 Frontend Responsibilities (React — Simple & Beginner-Friendly)

The React frontend is intentionally kept **minimal and functional**. It is a thin client: all business logic lives in the backend.

**Technology Choices:**
- **React 18** (Create React App or Vite for simplicity)
- **React Router v6** for navigation
- **Axios** for HTTP requests (with an interceptor for JWT attachment)
- **React Hook Form** for forms (minimal boilerplate)
- **Plain CSS or Bootstrap 5** — no complex UI framework

**Frontend Responsibilities:**
- Render pages and navigation based on user role
- Collect user input and send to API
- Display API responses (lists, detail views, status badges)
- Handle and display API error messages
- Store JWT in memory (access token) and HttpOnly cookie (refresh token)
- Redirect to login on 401 responses

**Frontend Does NOT:**
- Enforce sealed bid logic (this is backend-only)
- Validate business rules (only UX hints)
- Store sensitive data in localStorage

**Page Map:**

```
/login                  → Login page
/register               → Registration page
/dashboard              → Role-appropriate home

[CLIENT ROUTES]
/inquiries              → List my inquiries
/inquiries/new          → Create inquiry form
/inquiries/:id          → Inquiry detail (with offer count / revealed offers)
/inquiries/:id/edit     → Edit draft inquiry

[CONTRACTOR ROUTES]
/my-invitations         → List of inquiries I'm invited to
/inquiries/:id/submit   → Submit offer form
/my-offers              → List my submitted offers

[ADMIN ROUTES]
/admin/users            → User management
/admin/audit-logs       → Audit log viewer
```

---

### 4.3 Backend Responsibilities (Spring Boot — Robust & Professional)

**Technology Stack:**
- **Java 21** (LTS)
- **Spring Boot 3.x**
  - `spring-boot-starter-web` (REST API)
  - `spring-boot-starter-security` (JWT + RBAC)
  - `spring-boot-starter-data-mongodb` (Persistence)
  - `spring-boot-starter-validation` (Input validation)
  - `spring-boot-starter-actuator` (Health checks)
  - `springdoc-openapi-starter-webmvc-ui` (Swagger UI)
- **jjwt** (JWT library)
- **Lombok** (Boilerplate reduction)
- **MapStruct** (DTO mapping)
- **Testcontainers** (Integration tests with real MongoDB)

**Backend Responsibilities:**
- Enforce authentication and authorization on every request
- Execute all business logic (sealed bids, state transitions)
- Validate all incoming data
- Manage file uploads and storage
- Write audit log entries on every significant action
- Run scheduled jobs (deadline checking, status transitions)
- Expose OpenAPI documentation

---

### 4.4 REST API Design

**Base URL:** `http://localhost:8080/api/v1`

#### Authentication Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| POST | `/auth/register` | Register new user | No |
| POST | `/auth/login` | Login, returns JWT | No |
| POST | `/auth/refresh` | Refresh access token | Refresh cookie |
| POST | `/auth/logout` | Invalidate refresh token | Yes |

#### Inquiry Endpoints

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/inquiries` | List all accessible inquiries | CLIENT, ADMIN |
| POST | `/inquiries` | Create new inquiry | CLIENT |
| GET | `/inquiries/{id}` | Get inquiry detail | CLIENT, CONTRACTOR (if invited) |
| PUT | `/inquiries/{id}` | Update draft inquiry | CLIENT (owner) |
| PATCH | `/inquiries/{id}/publish` | Publish inquiry | CLIENT (owner) |
| PATCH | `/inquiries/{id}/cancel` | Cancel inquiry | CLIENT (owner), ADMIN |
| POST | `/inquiries/{id}/invitations` | Invite contractor by email | CLIENT (owner) |
| GET | `/inquiries/{id}/offers` | List offers (sealed/revealed) | CLIENT (owner), ADMIN |

#### Offer Endpoints

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/offers/mine` | List my submitted offers | CONTRACTOR |
| POST | `/inquiries/{id}/offers` | Submit offer | CONTRACTOR (invited) |
| PUT | `/offers/{id}` | Edit offer (before deadline) | CONTRACTOR (owner) |
| DELETE | `/offers/{id}` | Withdraw offer (before deadline) | CONTRACTOR (owner) |
| PATCH | `/offers/{id}/select-winner` | Mark as winner | CLIENT (inquiry owner) |

#### Admin Endpoints

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/admin/users` | List all users | ADMIN |
| PATCH | `/admin/users/{id}/deactivate` | Deactivate user | ADMIN |
| GET | `/admin/audit-logs` | Query audit logs | ADMIN |
| PATCH | `/admin/inquiries/{id}/extend-deadline` | Extend deadline | ADMIN |

---

### 4.5 MongoDB Schema Design

#### Collection: `users`

```json
{
  "_id": "ObjectId",
  "email": "string (unique, indexed)",
  "passwordHash": "string (BCrypt)",
  "fullName": "string",
  "institutionName": "string",
  "role": "enum [ADMIN, CLIENT, CONTRACTOR]",
  "active": "boolean (default: true)",
  "createdAt": "ISODate (UTC)",
  "updatedAt": "ISODate (UTC)",
  "lastLoginAt": "ISODate (UTC, nullable)"
}
```

**Indexes:** `email` (unique), `role`

---

#### Collection: `inquiries`

```json
{
  "_id": "ObjectId",
  "title": "string",
  "description": "string",
  "category": "string",
  "clientId": "ObjectId (ref: users)",
  "status": "enum [DRAFT, PUBLISHED, CLOSED, CANCELLED, ARCHIVED]",
  "deadline": "ISODate (UTC)",
  "attachments": [
    {
      "fileId": "string (UUID)",
      "originalName": "string",
      "mimeType": "string",
      "sizeBytes": "number",
      "uploadedAt": "ISODate"
    }
  ],
  "invitedContractorIds": ["ObjectId"],
  "winnerOfferId": "ObjectId (nullable)",
  "cancellationReason": "string (nullable)",
  "createdAt": "ISODate (UTC)",
  "updatedAt": "ISODate (UTC)"
}
```

**Indexes:** `clientId`, `status`, `deadline`, `invitedContractorIds`

---

#### Collection: `offers`

```json
{
  "_id": "ObjectId",
  "inquiryId": "ObjectId (ref: inquiries)",
  "contractorId": "ObjectId (ref: users)",
  "contractorName": "string (denormalized for audit clarity)",
  "price": "Decimal128",
  "currency": "string (default: PLN)",
  "validUntil": "ISODate",
  "notes": "string (nullable)",
  "status": "enum [SUBMITTED, WITHDRAWN, SELECTED, REJECTED]",
  "documents": [
    {
      "fileId": "string (UUID)",
      "originalName": "string",
      "mimeType": "string",
      "sizeBytes": "number",
      "uploadedAt": "ISODate"
    }
  ],
  "withdrawalReason": "string (nullable)",
  "submittedAt": "ISODate (UTC)",
  "updatedAt": "ISODate (UTC)"
}
```

**Indexes:** `inquiryId`, `contractorId`, compound `(inquiryId, contractorId)` unique

---

#### Collection: `audit_logs`

```json
{
  "_id": "ObjectId",
  "timestamp": "ISODate (UTC, indexed)",
  "actorId": "string (ObjectId or 'SYSTEM')",
  "actorRole": "string",
  "actorEmail": "string (denormalized)",
  "action": "string (e.g., INQUIRY_CREATED, OFFER_SUBMITTED, BID_REVEALED)",
  "entityType": "enum [INQUIRY, OFFER, USER, SYSTEM]",
  "entityId": "string (ObjectId)",
  "details": "object (action-specific metadata)",
  "ipAddress": "string",
  "userAgent": "string"
}
```

> ⚠️ **Critical:** This collection has **no update or delete operations** anywhere in the codebase. MongoDB user permissions should also deny these operations on this collection in production.

**Indexes:** `timestamp`, `actorId`, `entityId`, `action`

---

### 4.6 Docker Environment Structure

#### `docker-compose.yml`

```yaml
version: '3.9'

services:
  mongodb:
    image: mongo:7.0
    container_name: ezapytanie-mongo
    restart: unless-stopped
    environment:
      MONGO_INITDB_ROOT_USERNAME: ${MONGO_ROOT_USER}
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_ROOT_PASSWORD}
      MONGO_INITDB_DATABASE: ezapytanie
    volumes:
      - mongo_data:/data/db
      - ./mongo-init/init.js:/docker-entrypoint-initdb.d/init.js
    ports:
      - "27017:27017"
    networks:
      - ezapytanie-net

  backend:
    build:
      context: ./ezapytanie-backend
      dockerfile: Dockerfile
    container_name: ezapytanie-api
    restart: unless-stopped
    depends_on:
      - mongodb
    environment:
      SPRING_DATA_MONGODB_URI: mongodb://${MONGO_ROOT_USER}:${MONGO_ROOT_PASSWORD}@mongodb:27017/ezapytanie?authSource=admin
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION_MS: 900000
      REFRESH_TOKEN_EXPIRY_DAYS: 7
      FILE_STORAGE_PATH: /app/uploads
    volumes:
      - file_storage:/app/uploads
    ports:
      - "8080:8080"
    networks:
      - ezapytanie-net

  frontend:
    build:
      context: ./ezapytanie-frontend
      dockerfile: Dockerfile
    container_name: ezapytanie-ui
    restart: unless-stopped
    depends_on:
      - backend
    ports:
      - "80:80"
    networks:
      - ezapytanie-net

volumes:
  mongo_data:
  file_storage:

networks:
  ezapytanie-net:
    driver: bridge
```

#### Backend `Dockerfile`

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Frontend `Dockerfile`

```dockerfile
# Stage 1: Build React
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Serve via Nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

---

## 5. Mermaid Diagrams

### 5.1 Use Case Diagram

```mermaid
graph TD
    subgraph Actors
        A[👤 Admin]
        C[👤 Client]
        K[👤 Contractor]
        S[⚙️ System / Scheduler]
    end

    subgraph Authentication
        UC1[Register Account]
        UC2[Login / Logout]
        UC3[Refresh Token]
    end

    subgraph Inquiry Management
        UC4[Create Inquiry]
        UC5[Edit Draft Inquiry]
        UC6[Publish Inquiry]
        UC7[Invite Contractors]
        UC8[Cancel Inquiry]
        UC9[Extend Deadline]
        UC10[View Offer Count]
        UC11[View Revealed Offers]
        UC12[Select Winner]
        UC13[Export Inquiry Report]
    end

    subgraph Offer Management
        UC14[View Invitation]
        UC15[Submit Offer]
        UC16[Edit Own Offer]
        UC17[Withdraw Offer]
        UC18[View Own Offer]
    end

    subgraph Administration
        UC19[Manage Users]
        UC20[View Audit Logs]
        UC21[Auto-Close Expired Inquiries]
    end

    C --> UC1
    C --> UC2
    C --> UC4
    C --> UC5
    C --> UC6
    C --> UC7
    C --> UC8
    C --> UC10
    C --> UC11
    C --> UC12
    C --> UC13

    K --> UC1
    K --> UC2
    K --> UC14
    K --> UC15
    K --> UC16
    K --> UC17
    K --> UC18

    A --> UC9
    A --> UC19
    A --> UC20
    A --> UC8

    S --> UC21
    UC2 --> UC3
```

---

### 5.2 System Architecture Diagram

```mermaid
graph TB
    subgraph Browser["🖥️ User's Browser"]
        UI["React SPA<br/>(Vite + React Router)"]
    end

    subgraph Docker["🐳 Docker Network: ezapytanie-net"]
        subgraph Frontend["Container: ezapytanie-ui (Nginx:80)"]
            NGINX["Nginx<br/>Static File Server<br/>+ Reverse Proxy /api/*"]
        end

        subgraph Backend["Container: ezapytanie-api (Java:8080)"]
            direction TB
            CTRL["Controllers Layer<br/>(REST Endpoints)"]
            SEC["Security Layer<br/>(JWT Filter + RBAC)"]
            SVC["Service Layer<br/>(Business Logic)"]
            REPO["Repository Layer<br/>(Spring Data MongoDB)"]
            SCHED["Scheduler<br/>(@Scheduled — Deadline Check)"]
            AUDIT["AuditLog Service<br/>(Append-Only Writes)"]
            FILES["File Storage Service<br/>(UUID-named files)"]
        end

        subgraph Database["Container: ezapytanie-mongo (27017)"]
            MONGO["MongoDB 7.0"]
            COL1[("users")]
            COL2[("inquiries")]
            COL3[("offers")]
            COL4[("audit_logs")]
        end

        subgraph Volumes["Docker Volumes"]
            VOL1["mongo_data"]
            VOL2["file_storage"]
        end
    end

    UI -->|"HTTPS / REST JSON"| NGINX
    NGINX -->|"/api/* proxy"| CTRL
    CTRL --> SEC
    SEC --> SVC
    SVC --> REPO
    SVC --> AUDIT
    SVC --> FILES
    SCHED --> SVC
    REPO --> MONGO
    MONGO --- COL1
    MONGO --- COL2
    MONGO --- COL3
    MONGO --- COL4
    MONGO --> VOL1
    FILES --> VOL2
```

---

### 5.3 Sequence Diagram — Main Flow

```mermaid
sequenceDiagram
    actor Client
    actor Contractor
    participant API as Spring Boot API
    participant DB as MongoDB
    participant Sched as Scheduler

    Note over Client,Sched: Phase 1 — Create & Publish Inquiry

    Client->>API: POST /api/v1/inquiries (DRAFT)
    API->>DB: Insert inquiry {status: DRAFT}
    API->>DB: Insert AuditLog {action: INQUIRY_CREATED}
    API-->>Client: 201 Created {inquiryId}

    Client->>API: PATCH /api/v1/inquiries/{id}/publish
    API->>DB: Update inquiry {status: PUBLISHED}
    API->>DB: Insert AuditLog {action: INQUIRY_PUBLISHED}
    API-->>Client: 200 OK

    Client->>API: POST /api/v1/inquiries/{id}/invitations {email}
    API->>DB: Update inquiry.invitedContractorIds[]
    API->>DB: Insert AuditLog {action: CONTRACTOR_INVITED}
    API-->>Client: 200 OK

    Note over Client,Sched: Phase 2 — Contractor Submits Offer (Sealed)

    Contractor->>API: POST /api/v1/inquiries/{id}/offers {price, documents}
    API->>API: Validate: deadline not passed, contractor invited
    API->>DB: Insert offer {status: SUBMITTED, price: [SEALED]}
    API->>DB: Insert AuditLog {action: OFFER_SUBMITTED}
    API-->>Contractor: 201 Created {offerId}

    Client->>API: GET /api/v1/inquiries/{id}/offers
    API->>API: Check: inquiry.status == PUBLISHED → SEAL prices
    API-->>Client: 200 OK {offers: [{price: null, documents: []}]}

    Note over Client,Sched: Phase 3 — Deadline Passes, Bids Revealed

    Sched->>API: Scheduled check (every 60s)
    API->>DB: Query {status: PUBLISHED, deadline: {$lte: now}}
    DB-->>API: [inquiry_id_123]
    API->>DB: Update inquiry {status: CLOSED}
    API->>DB: Insert AuditLog {actor: SYSTEM, action: AUTO_CLOSE}

    Client->>API: GET /api/v1/inquiries/{id}/offers
    API->>API: Check: inquiry.status == CLOSED → REVEAL prices
    API-->>Client: 200 OK {offers: [{price: 15000, documents: [...]}]}

    Note over Client,Sched: Phase 4 — Winner Selection

    Client->>API: PATCH /api/v1/offers/{offerId}/select-winner
    API->>DB: Update offer {status: SELECTED}
    API->>DB: Update all other offers {status: REJECTED}
    API->>DB: Update inquiry {winnerOfferId, status: ARCHIVED}
    API->>DB: Insert AuditLog {action: WINNER_SELECTED}
    API-->>Client: 200 OK
```

---

## 6. Professional Backlog

> **Priority:** 🔴 Critical / 🟠 High / 🟡 Medium / 🟢 Low  
> **Difficulty:** 1 (trivial) → 5 (complex)

---

### Epic 1: Project Setup

| # | Title | Priority | Difficulty | Acceptance Criteria |
|---|-------|----------|------------|---------------------|
| SETUP-01 | Initialize Spring Boot project (Spring Initializr) | 🔴 Critical | 1 | Project runs with `mvn spring-boot:run`; health endpoint returns 200 |
| SETUP-02 | Initialize React project with Vite | 🔴 Critical | 1 | `npm run dev` serves app on localhost:5173 |
| SETUP-03 | Configure Docker Compose (MongoDB + Backend + Frontend) | 🔴 Critical | 3 | `docker compose up` starts all 3 containers; backend connects to MongoDB |
| SETUP-04 | Configure `.env` file and secret management | 🔴 Critical | 2 | No secrets in source code; `.env.example` committed; `.env` gitignored |
| SETUP-05 | Configure OpenAPI / Swagger UI (Springdoc) | 🟠 High | 1 | Swagger UI accessible at `/swagger-ui.html` |
| SETUP-06 | Set up Git repository with branching strategy | 🔴 Critical | 1 | `main` and `develop` branches created; `.gitignore` configured for Java and Node |

---

### Epic 2: Authentication

| # | Title | Priority | Difficulty | Acceptance Criteria |
|---|-------|----------|------------|---------------------|
| AUTH-01 | Implement User registration endpoint | 🔴 Critical | 2 | POST `/auth/register` creates user; password BCrypt-hashed; email unique validation returns 409 |
| AUTH-02 | Implement JWT login endpoint | 🔴 Critical | 3 | POST `/auth/login` returns `accessToken` (15min) and sets `refreshToken` HttpOnly cookie |
| AUTH-03 | Implement JWT validation filter | 🔴 Critical | 3 | All protected endpoints return 401 without valid JWT; valid JWT allows access |
| AUTH-04 | Implement refresh token rotation | 🟠 High | 3 | POST `/auth/refresh` returns new access token; old refresh token invalidated |
| AUTH-05 | Implement logout endpoint | 🟠 High | 2 | POST `/auth/logout` deletes refresh token from DB; cookie cleared |
| AUTH-06 | Implement role-based access control | 🔴 Critical | 3 | CLIENT-only endpoints return 403 when called with CONTRACTOR token; admin endpoints inaccessible to non-admins |
| AUTH-07 | Frontend: Login page and JWT storage | 🔴 Critical | 2 | Access token stored in memory (not localStorage); Axios interceptor attaches Bearer token |
| AUTH-08 | Frontend: Protected route wrapper | 🟠 High | 2 | Unauthenticated users redirected to `/login`; role-appropriate redirect post-login |

---

### Epic 3: Database

| # | Title | Priority | Difficulty | Acceptance Criteria |
|---|-------|----------|------------|---------------------|
| DB-01 | Create MongoDB indexes via Spring Data | 🔴 Critical | 2 | Unique index on `users.email`; compound unique on `(inquiryId, contractorId)` in offers |
| DB-02 | Implement AuditLog repository (append-only) | 🔴 Critical | 2 | AuditLogRepository has no `save(update)` or `delete` methods; only `save(new)` and `findBy*` |
| DB-03 | Configure MongoDB authentication in Docker | 🟠 High | 2 | MongoDB only accessible with credentials; no unauthenticated connections accepted |
| DB-04 | Implement MongoDB change stream or scheduled status sync (optional) | 🟢 Low | 4 | Real-time status updates without polling (Phase 2) |

---

### Epic 4: Request (Inquiry) Management

| # | Title | Priority | Difficulty | Acceptance Criteria |
|---|-------|----------|------------|---------------------|
| RFQ-01 | Implement Create Inquiry (POST /inquiries) | 🔴 Critical | 2 | Returns 201; inquiry created with status DRAFT; audit log entry written |
| RFQ-02 | Implement Get Inquiry by ID | 🔴 Critical | 1 | Returns 200 with full detail; 403 if contractor not in invitedList; 404 if not found |
| RFQ-03 | Implement List Inquiries (with filters) | 🟠 High | 2 | CLIENT sees own inquiries; ADMIN sees all; filterable by status |
| RFQ-04 | Implement Publish Inquiry | 🔴 Critical | 2 | Status transitions DRAFT → PUBLISHED; only owner can publish; deadline must be in the future |
| RFQ-05 | Implement Cancel Inquiry | 🟠 High | 2 | Status transitions to CANCELLED; reason required; existing offers marked REJECTED |
| RFQ-06 | Implement Invite Contractor | 🔴 Critical | 2 | Contractor email validated against users; added to `invitedContractorIds`; audit log written |
| RFQ-07 | Implement Deadline Scheduler | 🔴 Critical | 3 | @Scheduled job runs every 60s; PUBLISHED inquiries past deadline transition to CLOSED; SYSTEM audit log written |
| RFQ-08 | Implement Extend Deadline (Admin only) | 🟡 Medium | 2 | Admin-only; new deadline must be after current deadline; justification required; audit log written |
| RFQ-09 | Frontend: Inquiry list page | 🟠 High | 2 | Shows status badge, deadline, and offer count; pagination works |
| RFQ-10 | Frontend: Create/Edit inquiry form | 🟠 High | 2 | Validation errors displayed inline; file upload works; form submits to API |
| RFQ-11 | Frontend: Inquiry detail page | 🟠 High | 2 | Shows status; shows sealed offer count or revealed offer list depending on status |

---

### Epic 5: Offer Management

| # | Title | Priority | Difficulty | Acceptance Criteria |
|---|-------|----------|------------|---------------------|
| OFF-01 | Implement Submit Offer (POST /inquiries/{id}/offers) | 🔴 Critical | 3 | Returns 201; validates contractor is invited; validates deadline not passed; enforces one offer per contractor per inquiry |
| OFF-02 | Implement Sealed Bid response mapper | 🔴 Critical | 3 | When inquiry is PUBLISHED: price=null, documents=[] in response; verified by integration test |
| OFF-03 | Implement Edit Offer (PUT /offers/{id}) | 🟠 High | 2 | Only offer owner; only before deadline; audit log entry written |
| OFF-04 | Implement Withdraw Offer (DELETE /offers/{id}) | 🟠 High | 2 | Status → WITHDRAWN; reason required; cannot be undone |
| OFF-05 | Implement List Offers for Inquiry | 🔴 Critical | 2 | CLIENT (owner): see sealed list before deadline, full list after; CONTRACTOR: see only own offer |
| OFF-06 | Implement Select Winner | 🟠 High | 2 | CLIENT owner marks one offer SELECTED; all others REJECTED; inquiry → ARCHIVED; audit log written |
| OFF-07 | Implement File Upload for Offers | 🟠 High | 4 | Files stored with UUID names; MIME type + size validated; file served via secure endpoint |
| OFF-08 | Frontend: Submit offer form | 🔴 Critical | 2 | Price input, document upload, validity date; confirmation before submit |
| OFF-09 | Frontend: Offer list (Client view) | 🟠 High | 2 | Shows "N offers sealed" before deadline; full price table after; Select Winner button |
| OFF-10 | Frontend: My offers page (Contractor view) | 🟡 Medium | 1 | Lists own offers with status badge; links to inquiry detail |

---

### Epic 6: Reporting & Audit

| # | Title | Priority | Difficulty | Acceptance Criteria |
|---|-------|----------|------------|---------------------|
| REP-01 | Implement Audit Log writer service | 🔴 Critical | 2 | Every Inquiry and Offer state change writes to `audit_logs`; actor, entity, action, IP all captured |
| REP-02 | Implement Audit Log query endpoint (Admin) | 🟠 High | 2 | Filterable by entityId, actorId, action, date range; paginated |
| REP-03 | Frontend: Audit log viewer (Admin) | 🟡 Medium | 2 | Table view with filters; timestamp, actor, action, entity displayed |
| REP-04 | Implement Inquiry export (JSON summary) | 🟡 Medium | 2 | GET `/inquiries/{id}/export` returns structured JSON with inquiry + all revealed offers |
| REP-05 | Implement basic health/metrics endpoint | 🟢 Low | 1 | Spring Actuator `/actuator/health` returns UP with DB status |

---

## 7. Engineering Best Practices

### 7.1 Repository Structure (Split-Repo Approach)

```
ezapytanie/                         # Root workspace (optional monorepo wrapper)
│
├── ezapytanie-backend/             # Spring Boot project
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/ezapytanie/
│   │   │   │   ├── EzapytanieApplication.java
│   │   │   │   ├── config/
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   ├── MongoConfig.java
│   │   │   │   │   └── OpenApiConfig.java
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   ├── InquiryController.java
│   │   │   │   │   ├── OfferController.java
│   │   │   │   │   └── AdminController.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── AuthService.java
│   │   │   │   │   ├── InquiryService.java
│   │   │   │   │   ├── OfferService.java
│   │   │   │   │   ├── AuditLogService.java
│   │   │   │   │   ├── FileStorageService.java
│   │   │   │   │   └── DeadlineSchedulerService.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   ├── InquiryRepository.java
│   │   │   │   │   ├── OfferRepository.java
│   │   │   │   │   └── AuditLogRepository.java
│   │   │   │   ├── model/
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Inquiry.java
│   │   │   │   │   ├── Offer.java
│   │   │   │   │   └── AuditLog.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── request/
│   │   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   │   ├── CreateInquiryRequest.java
│   │   │   │   │   │   └── SubmitOfferRequest.java
│   │   │   │   │   └── response/
│   │   │   │   │       ├── AuthResponse.java
│   │   │   │   │       ├── InquiryResponse.java
│   │   │   │   │       └── OfferResponse.java
│   │   │   │   ├── mapper/
│   │   │   │   │   ├── InquiryMapper.java
│   │   │   │   │   └── OfferResponseMapper.java    ← Sealed Bid logic here
│   │   │   │   ├── security/
│   │   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   │   ├── exception/
│   │   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   │   ├── DeadlinePassedException.java
│   │   │   │   │   └── SealedBidViolationException.java
│   │   │   │   └── enums/
│   │   │   │       ├── UserRole.java
│   │   │   │       ├── InquiryStatus.java
│   │   │   │       └── OfferStatus.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── application-docker.yml
│   │   └── test/
│   │       └── java/com/ezapytanie/
│   │           ├── service/           # Unit tests
│   │           └── controller/        # Integration tests (Testcontainers)
│   ├── Dockerfile
│   └── pom.xml
│
├── ezapytanie-frontend/            # React (Vite) project
│   ├── src/
│   │   ├── main.jsx
│   │   ├── App.jsx
│   │   ├── api/
│   │   │   ├── axiosInstance.js    # Axios + JWT interceptor
│   │   │   ├── authApi.js
│   │   │   ├── inquiryApi.js
│   │   │   └── offerApi.js
│   │   ├── pages/
│   │   │   ├── LoginPage.jsx
│   │   │   ├── RegisterPage.jsx
│   │   │   ├── DashboardPage.jsx
│   │   │   ├── InquiryListPage.jsx
│   │   │   ├── InquiryDetailPage.jsx
│   │   │   ├── CreateInquiryPage.jsx
│   │   │   ├── SubmitOfferPage.jsx
│   │   │   └── AdminAuditLogPage.jsx
│   │   ├── components/
│   │   │   ├── ProtectedRoute.jsx
│   │   │   ├── StatusBadge.jsx
│   │   │   ├── OfferTable.jsx
│   │   │   └── FileUpload.jsx
│   │   ├── context/
│   │   │   └── AuthContext.jsx
│   │   └── utils/
│   │       └── dateUtils.js
│   ├── nginx.conf
│   ├── Dockerfile
│   └── package.json
│
├── docker-compose.yml
├── .env.example
├── .gitignore
└── PROJECT_SPECIFICATION.md       ← This file
```

---

### 7.2 Naming Conventions

#### Java / Spring Boot

| Element | Convention | Example |
|---------|-----------|---------|
| Classes | `PascalCase` | `InquiryService`, `OfferResponseDto` |
| Methods | `camelCase` | `submitOffer()`, `findByInquiryId()` |
| Constants | `UPPER_SNAKE_CASE` | `MAX_FILE_SIZE_BYTES` |
| MongoDB collections | `snake_case` | `audit_logs`, `inquiries` |
| MongoDB fields | `camelCase` (Spring default) | `invitedContractorIds` |
| Packages | `lowercase` | `com.ezapytanie.service` |
| DTOs | `*Request` / `*Response` | `CreateInquiryRequest`, `OfferResponse` |

#### React / JavaScript

| Element | Convention | Example |
|---------|-----------|---------|
| Components | `PascalCase.jsx` | `InquiryDetailPage.jsx` |
| Hooks | `use*` prefix | `useAuth()`, `useInquiry()` |
| API modules | `*Api.js` suffix | `inquiryApi.js` |
| CSS classes | `kebab-case` | `.offer-table-row` |
| Constants | `UPPER_SNAKE_CASE` | `API_BASE_URL` |

---




### 7.4 Error Handling Standards

#### Backend — Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(new ErrorResponse("FORBIDDEN", "Access denied"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
            .stream().map(e -> e.getField() + ": " + e.getDefaultMessage()).toList();
        return ResponseEntity.status(400).body(new ErrorResponse("VALIDATION_ERROR", errors.toString()));
    }

    @ExceptionHandler(DeadlinePassedException.class)
    public ResponseEntity<ErrorResponse> handleDeadlinePassed(DeadlinePassedException ex) {
        return ResponseEntity.status(409).body(new ErrorResponse("DEADLINE_PASSED", ex.getMessage()));
    }
    
    // Catch-all: never leak stack traces to client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(500).body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
```

#### Standard Error Response DTO

```json
{
  "code": "VALIDATION_ERROR",
  "message": "price: must be greater than 0",
  "timestamp": "2026-05-03T10:30:00Z"
}
```

#### Frontend Error Handling

```javascript
// axiosInstance.js — centralized error handling
axiosInstance.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      // Token expired — attempt refresh or redirect to login
      authService.logout();
      window.location.href = '/login';
    }
    // Pass error to component for display
    return Promise.reject(error.response?.data ?? { code: 'NETWORK_ERROR', message: 'No connection' });
  }
);
```

---

### 7.5 Testing Standards

#### Unit Tests (Service Layer)

```java
// Target: Pure business logic, no Spring context, no DB
// Naming: MethodName_StateUnderTest_ExpectedBehavior
@Test
void submitOffer_WhenDeadlinePassed_ShouldThrowDeadlinePassedException() { ... }

@Test
void getOffersForInquiry_WhenInquiryPublished_ShouldReturnSealedPrices() { ... }
```

#### Integration Tests (Controller Layer)

```java
// Use @SpringBootTest + Testcontainers (real MongoDB)
// Test full HTTP request/response cycle
@Test
void postOffer_WithValidJwt_ShouldReturn201AndSealPricesForClient() { ... }
```

**Coverage Target:** `>= 70%` on `service` and `mapper` packages.

---

*End of PROJECT_SPECIFICATION.md — eZapytanie v1.0*

> 💡 **For the thesis author:** This document serves as your single source of truth. Update it as architectural decisions evolve. Version it with Git tags alongside your code releases. Good luck! 🚀
