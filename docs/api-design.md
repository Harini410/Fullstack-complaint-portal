# REST API Design Specification

## 1. Architectural Guidelines

The REST APIs follow strict RESTful conventions, utilizing appropriate HTTP methods, status codes, and JSON data formats.

### HTTP Method Semantics
- `GET`: Safe, idempotent retrieval of resources.
- `POST`: Creation of subordinate resources.
- `PUT`: Complete idempotent replacement of an existing resource.
- `PATCH`: Partial modification of resource state (e.g. status transition, assignment).
- `DELETE`: Idempotent removal of a resource.

### Standard HTTP Status Codes
| Status | Meaning | Usage |
| :--- | :--- | :--- |
| `200 OK` | Request succeeded | Standard response for successful `GET`, `PUT`, `PATCH` |
| `201 Created` | Resource created | Returned by `POST` on registration, complaint creation, comments |
| `204 No Content` | Success, no body | Returned by `DELETE` operations |
| `400 Bad Request` | Validation failure | Malformed input, constraint violations, invalid status transition |
| `401 Unauthorized`| Authentication missing | Invalid/expired JWT token or missing credentials |
| `403 Forbidden` | Access denied | User authenticated but role lacks required privilege |
| `404 Not Found` | Missing resource | Resource ID does not exist |
| `409 Conflict` | Duplicate resource | Username or email already registered |
| `500 Server Error`| Internal failure | Unhandled exceptions (logged with stack trace) |

---

## 2. API Endpoints Catalog

### Authentication & Profile (`/api/auth`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Public | Register new user account with BCrypt password |
| `POST` | `/api/auth/login` | Public | Authenticate user credentials and return JWT |
| `GET` | `/api/auth/me` | Authenticated | Retrieve current user profile |

### Complaints (`/api/complaints`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/complaints` | Public / Auth | List complaints (array or paged with filters) |
| `GET` | `/api/complaints/{id}` | Public / Auth | Retrieve single complaint by ID |
| `POST` | `/api/complaints` | Public / Auth | Create new complaint |
| `PUT` | `/api/complaints/{id}` | Authenticated | Update complaint description, category, or priority |
| `PATCH`| `/api/complaints/{id}/status` | Admin / Agent | Update status (`OPEN`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`) |
| `PATCH`| `/api/complaints/{id}/assign` | Admin | Assign support agent to ticket |
| `DELETE`| `/api/complaints/{id}` | Auth / Owner | Remove complaint |
| `GET` | `/api/complaints/{id}/history`| Public / Auth | Retrieve audit trail of changes |

### Categories (`/api/categories`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/categories` | Public | List categories (cached in Redis) |
| `POST` | `/api/categories` | Admin | Create new category (evicts cache) |
| `DELETE`| `/api/categories/{id}` | Admin | Delete category (evicts cache) |

### Comments (`/api/complaints/{id}/comments`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/complaints/{id}/comments` | Public / Auth | Get comments on a complaint |
| `POST` | `/api/complaints/{id}/comments` | Authenticated | Post a comment on a complaint |

### Admin Dashboard & Users (`/api/admin`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/statistics` | Admin | Aggregate counts by status & priority (Redis cached) |
| `GET` | `/api/admin/users` | Admin | List all registered users |
| `GET` | `/api/admin/support-agents` | Admin | List support agents available for assignment |
| `PATCH`| `/api/admin/users/{id}/role` | Admin | Change user role (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_SUPPORT_AGENT`) |

---

## 3. Request & Response Payloads

### 1. User Registration
`POST /api/auth/register`
```json
{
  "username": "johndoe",
  "email": "johndoe@example.com",
  "password": "SecurePassword123",
  "fullName": "John Doe",
  "role": "ROLE_USER"
}
```
**Response (`201 Created`):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": 4,
  "username": "johndoe",
  "email": "johndoe@example.com",
  "role": "ROLE_USER",
  "expiresInMs": 86400000
}
```

### 2. Create Complaint
`POST /api/complaints`
```json
{
  "title": "Low water pressure in Sector 4",
  "description": "Pressure dropped significantly since morning, making upper floors dry.",
  "category": "Water",
  "priority": "HIGH"
}
```
**Response (`201 Created`):**
```json
{
  "id": 3,
  "title": "Low water pressure in Sector 4",
  "description": "Pressure dropped significantly since morning, making upper floors dry.",
  "category": "Water",
  "categoryId": 2,
  "status": "OPEN",
  "priority": "HIGH",
  "createdById": 4,
  "createdByName": "John Doe",
  "assignedToId": null,
  "assignedToName": null,
  "createdAt": "2026-09-25T10:15:30",
  "updatedAt": "2026-09-25T10:15:30",
  "commentCount": 0
}
```

### 3. Standardized Error Response (RFC 7807 Pattern)
`POST /api/complaints` (Validation Failure, HTTP 400)
```json
{
  "timestamp": "2026-09-25T10:16:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed. Please check the 'validationErrors' field for details.",
  "path": "/api/complaints",
  "validationErrors": {
    "description": "Description is required"
  }
}
```
