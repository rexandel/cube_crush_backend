# Service Contracts & Architecture

This document describes the microservices architecture, API contracts, and the interaction flow within the Cube Crush Backend.

## Architecture Overview

The system is built using a microservices architecture with the following components:

1.  **API Gateway**: Entry point for all client requests. Handles routing, authentication, and load balancing.
2.  **Auth Service**: Manages user registration, authentication, and session management (JWT).
3.  **User Service**: Manages user profiles and account data.
4.  **Game Service**: Manages game scores, leaderboards, and statistics.
5.  **Eureka Server**: Service discovery registry.

## API Gateway Interaction

The API Gateway (running on port `8080`) is the only entry point accessible to external clients.

### Authentication Flow

1.  **Public Endpoints**: Routes like `/auth/login` and `/auth/register` are open and do not require a token.
2.  **Protected Endpoints**: All other routes (e.g., `/users/me`, `/game/score`) require a valid JWT Access Token.
3.  **Token Validation**:
    *   The Gateway intercepts requests with the `Authorization: Bearer <token>` header.
    *   It validates the JWT signature and expiration.
    *   It extracts the `userId` and `nickname` (subject) from the token claims.
4.  **Context Propagation**:
    *   If valid, the Gateway adds internal headers to the request before forwarding it to the downstream service:
        *   `X-User-Id`: The ID of the authenticated user.
        *   `X-User-Email`: The nickname of the authenticated user (mapped from the token subject).

### Routing Table

| Path Pattern | Method | Target Service | Auth Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `/api/v1/auth/register` | POST | `auth-service` | No | Register new user |
| `/api/v1/auth/login` | POST | `auth-service` | No | Login user |
| `/api/v1/auth/refresh` | POST | `auth-service` | No | Refresh access token |
| `/api/v1/auth/logout` | POST | `auth-service` | Yes | Logout user |
| `/api/v1/auth/validate` | POST | `auth-service` | Yes | Validate token |
| `/api/v1/auth/health` | GET | `auth-service` | No | Health check |
| `/api/v1/users/**` | * | `user-service` | Yes | User profile management |
| `/api/v1/game/**` | * | `game-service` | Mixed* | Game logic (*Top/History might be public depending on config, currently protected via Gateway filter logic) |

---

## Service Contracts

### 1. Auth Service (`auth-service`)

**Responsibility**: Identity management, token generation, session tracking.

**Database Schema**:
*   `user_sessions`: Tracks active refresh tokens and their validity.
*   `revoked_tokens`: Blacklist for revoked JWTs (JTI).

**Public API**:
*   `POST /register`: Accepts `nickname`, `password`. Returns `AuthResponse` (tokens + profile).
*   `POST /login`: Accepts `nickname`, `password`. Returns `AuthResponse`.
*   `POST /refresh`: Accepts `refreshToken`. Returns `AuthResponse`.
*   `POST /logout`: Invalidates the current session.

**Internal API** (Used by Gateway/Other services):
*   `POST /validate`: Validates a token and returns user details.

**Key DTOs**:
```java
// AuthResponse
{
  "accessToken": "eyJ...",
  "refreshToken": "d9b...",
  "userProfile": { "id": 1, "nickname": "Player1", "createdAt": "..." }
}
```

### 2. User Service (`user-service`)

**Responsibility**: User profile data, password management.

**Database Schema**:
*   `users`: Stores `id`, `nickname`, `password_hash`, `created_at`.

**Public API** (Protected by Gateway):
*   `GET /me`: Returns `UserProfile`. Requires `X-User-Id` header.
*   `PATCH /me/nickname`: Updates nickname.
*   `PATCH /me/password`: Updates password.

**Internal API** (Hidden, for inter-service communication):
*   `POST /api/v1/system/users`: Creates a user (called by Auth Service during registration).
*   `GET /api/v1/system/users/{id}`: Get user by ID.
*   `GET /api/v1/system/users/by-nickname/{nickname}`: Get user by nickname.
*   `POST /api/v1/system/users/validate-credentials`: Verify password hash.

### 3. Game Service (`game-service`)

**Responsibility**: Score tracking, leaderboards, statistics.

**Database Schema**:
*   `scores`: Stores individual game results (`user_id`, `score`, `achieved_at`).
*   `top_players` (Materialized View): Global leaderboard.
*   `user_stats` (Materialized View): Aggregated user statistics.

**Public API**:
*   `POST /score`: Submit new score. Requires `X-User-Id`.
*   `GET /top`: Get global leaderboard.
*   `GET /stats`: Get current user's stats. Requires `X-User-Id`.
*   `GET /history`: Get current user's game history. Requires `X-User-Id`.

**Data Flow - Score Submission**:
1.  Client sends `POST /api/v1/game/score` with JSON `{ "score": 100 }`.
2.  Gateway validates JWT, extracts `userId=123`.
3.  Gateway forwards to Game Service with header `X-User-Id: 123`.
4.  Game Service saves score to `scores` table.
5.  Database Trigger `refresh_views_after_score` fires.
6.  `user_stats` view is refreshed.
7.  If score is a new record, `top_players` view is refreshed.

## Database Consistency

*   **User IDs**: The `users` table in `user-service` is the source of truth for User IDs.
*   **Foreign Keys**: `auth-service` and `game-service` store `user_id` but do not enforce foreign key constraints at the database level across microservices (as they have separate DB contexts/schemas in a real microservices deployment, though currently sharing a physical DB instance).
*   **Replication**: Data is not replicated; services query `user-service` via REST if they need user details (e.g., Game Service fetching nickname for stats if missing).
