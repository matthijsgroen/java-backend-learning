# Architecture To-Do

This document tracks what still needs to be built to reach the goals described in `ARCHITECTURE.md`.
Items are grouped by theme, ordered roughly by dependency (foundations first).

---

## 1. Users & Identity

The entire ownership, sharing, and permission model depends on a user identity concept that does not yet exist.

- [x] **Create `User` entity** — id (UUID), username, email, passwordHash, createdAt, modifiedAt
- [x] **Create `UserRepository`** with lookup by username/email
- [x] **Create `UserService`** with registration and profile management
- [x] **Expose `POST /users` (register) and `GET /users/me`** endpoints
  - `GET /users/me` currently uses `X-User-Id` header as a stub; will be replaced with JWT subject in Story 2
- [ ] **Add `owner` (User FK) to `Widget` entity** — every widget must have an owner
- [ ] **Add `owner` (User FK) to `Dashboard` entity** — every dashboard must have an owner

---

## 2. Authentication & Spring Security

All management endpoints are currently unprotected. Authentication must be in place before ownership and sharing can be enforced.

- [ ] **Add `spring-boot-starter-security` to `pom.xml`**
- [ ] **Implement `SecurityConfig`** — define public vs. protected endpoint rules
  - `PasswordEncoder` bean is already available in `user/PasswordEncoderConfig.java`; move or `@Import` it here
- [ ] **Choose and implement an auth mechanism** (JWT, session, or OAuth2)
- [ ] **Protect all mutating endpoints** (`POST`, `PUT`, `DELETE` on `/widget` and `/dashboards`) — require authentication
- [ ] **Return `401 Unauthorized`** for unauthenticated requests to protected endpoints
- [ ] **Return `403 Forbidden`** when an authenticated user attempts an action they are not allowed to perform
- [ ] **Replace `X-User-Id` header stub in `GET /users/me`** with JWT subject from `SecurityContextHolder`

---

## 3. Groups & Group Membership

Groups allow widgets to be shared across multiple users.

- [ ] **Create `Group` entity** — id, name, createdAt
- [ ] **Create `UserGroup` join entity** — userId, groupId, role (e.g. MEMBER / ADMIN)
- [ ] **Create `GroupRepository` and `GroupService`**
- [ ] **Expose group management endpoints** — create group, list groups, add/remove members
- [ ] **Expose `GET /groups/me`** — list groups the authenticated user belongs to

---

## 4. Widget Ownership & Sharing / Permissions

With users and groups in place, widgets need ownership and a sharing mechanism.

- [ ] **Record the authenticated caller as owner when `POST /widget` is called**
- [ ] **Create `WidgetShare` entity** — widgetId, groupId, permission (READ\_ONLY / READ\_WRITE)
- [ ] **Expose `POST /widget/{id}/share`** — share a widget with a group (owner only)
- [ ] **Expose `DELETE /widget/{id}/share/{groupId}`** — revoke a share (owner only)
- [ ] **Enforce read-only access** for group members on shared widgets — reject `PUT` and `DELETE` with `403`
- [ ] **Prevent deletion of shared widgets** — if a widget has one or more active `WidgetShare` entries, `DELETE /widget/{id}` must be rejected with `409 Conflict`; the owner must revoke all shares before the widget can be deleted
- [ ] **Add `ownerId` / `ownerName` to `GetWidgetResponse`** so consumers can see who owns a widget
- [ ] **Add `GET /widgets`** — list widgets owned by the authenticated user and shared with their groups; support filtering by `widgetType` and root-only

---

## 5. Dashboard CRUD

Only `GET /dashboards` (list) exists. Full lifecycle management is missing.

- [ ] **`POST /dashboards`** — create a dashboard (name, optional config); record owner; return created dashboard
- [ ] **`GET /dashboards/{id}`** — fetch a single dashboard with its root widget tree
- [ ] **`PUT /dashboards/{id}`** — rename or update a dashboard's top-level config (owner only)
- [ ] **`DELETE /dashboards/{id}`** — soft-delete a dashboard by setting `deletedAt` (owner only)
- [ ] **Add a `DashboardService`** — move business logic out of the controller, which currently calls the repository directly

---

## 6. Dashboard as Widget (Unified Model)

The architecture states a dashboard *is* a widget. The current `Dashboard` and `Widget` entities exist in parallel with no linkage.

- [ ] **Link `Dashboard` to `Widget`** — add a `rootWidgetId` FK on `Dashboard` pointing to the root widget of the hierarchy, **or** model the dashboard itself as a `Widget` of `widgetType = "dashboard"` and retire the separate `Dashboard` entity
- [ ] **Ensure deleting a dashboard cascades to its widget tree**
- [ ] **Update `GET /dashboards/{id}`** to return the full nested widget structure via the widget hierarchy system

---

## 7. Widget Full Update

`PUT /widget/{id}` currently only supports re-parenting. There is no way to update a widget's configuration, secrets, model, or endpoints after creation.

- [ ] **Expand `UpdateWidgetRequest`** to accept `widgetType`, `version`, `configuration`, `configurationModel`, and `dataEndpoints` (all optional / patch-style)
- [ ] **Update `WidgetService.updateWidget`** to re-run `ConfigurationValidator` and `ConfigurationExtractor` on changed fields
- [ ] **Restrict full-update to the widget owner** — return `403` for non-owners

---

## 8. Widget Soft-Delete & Cleanup

Widgets are currently hard-deleted immediately. The `Dashboard` entity has a `deletedAt` field (soft-delete) but widgets do not.

- [ ] **Add `deletedAt` to `Widget` entity**
- [ ] **Change `DELETE /widget/{id}`** to set `deletedAt` instead of physically removing the row
- [ ] **Update all repository queries** (`findByParentId`, `findChildIdsByParentId`, `findAllDescendantIds`, etc.) to filter out soft-deleted widgets
- [ ] **Add a scheduled cleanup job** that hard-deletes widgets (and dashboard) whose `deletedAt` is older than a configurable retention period

---

## 9. Request Caching for Widget Data Endpoints

The `cache` field on `DataEndpointModelItem` is stored but completely ignored at runtime.

- [ ] **Add a caching layer** to `WidgetEndpointsController.getWidgetEndpoint` — check the cache before executing steps; store the result with the configured TTL after execution
- [ ] **Choose a cache backend** (Caffeine for in-process, Redis for distributed)
- [ ] **Define a cache key** — e.g. `widgetId + endpointName`
- [ ] **Invalidate the cache** when a widget's configuration or endpoints are updated

---

## 10. Rate Limiting for Widget Data Endpoints

The architecture mentions rate limits alongside cache settings, but there is no model or enforcement for them.

- [ ] **Add a `rateLimit` field to `DataEndpointModelItem`** (e.g. requests per minute; `0` = unlimited)
- [ ] **Enforce rate limiting** in `WidgetEndpointsController` before step execution — return `429 Too Many Requests` when exceeded
- [ ] **Choose a rate-limit backend** (Bucket4j + Caffeine, or Redis)

---

## 11. Shareable Dashboard URLs

There is no way to generate or resolve a public share link for a dashboard.

- [ ] **Add `shareToken` (UUID) to `Dashboard` entity** — nullable; generated on demand
- [ ] **`POST /dashboards/{id}/share`** — generate and persist a share token; return the shareable URL
- [ ] **`DELETE /dashboards/{id}/share`** — revoke the share token
- [ ] **`GET /dashboards/public/{token}`** — resolve a share token and return the dashboard (no auth required for public dashboards)
- [ ] **Add `findByShareToken` to `DashboardRepository`**

---

## 12. Password-Protected Public Dashboards

Public dashboards can optionally require a password.

- [ ] **Add `isPublic` (boolean) and `passwordHash` (String) to `Dashboard` entity**
- [ ] **`PUT /dashboards/{id}/password`** — set or update the dashboard password (hashed before storage; owner only)
- [ ] **`DELETE /dashboards/{id}/password`** — remove the password requirement (owner only)
- [ ] **Enforce password check** on `GET /dashboards/public/{token}` when `passwordHash` is set — accept password in request body or header; return `401` on mismatch

---

## 13. CORS Configuration

Cross-origin requests from the React frontend are blocked by default.

- [ ] **Add a `WebMvcConfigurer` (or Spring Security CORS config)** to allow requests from the frontend origin
- [ ] **Expose allowed origins via `application.properties`** (e.g. `dashboarding.cors.allowed-origins=http://localhost:3000`)

---

## 14. Secrets Encryption Hardening

The current `EncryptionUtil` uses AES in ECB mode with a hardcoded salt — known cryptographic weaknesses.

- [ ] **Replace ECB mode with AES/GCM/NoPadding** — include a random IV per encryption operation
- [ ] **Use a random per-record salt** rather than the hardcoded `"widget-encryption-salt"` string
- [ ] **Derive the encryption key from a server-side master secret** (e.g. from `application.properties` / environment variable), not from the `widgetType`
- [ ] **Store IV + salt alongside the ciphertext** in the `secretsConfiguration` column
- [ ] **Wire up `ConfigurationFieldStorage` enum** (`PLAIN` / `ENCRYPTED`) so storage type is controlled per field in the `configurationModel`, not assumed from scope alone

---

## 15. Database Migrations

`ddl-auto=update` is suitable for development only. Production deployments require versioned, controlled schema migrations.

- [ ] **Add Flyway (or Liquibase) to `pom.xml`**
- [ ] **Write an initial baseline migration** capturing the current schema
- [ ] **Set `spring.jpa.hibernate.ddl-auto=validate`** once Flyway is managing the schema
- [ ] **Add a migration for every future schema change** introduced by items above

---

## 16. Documentation Hygiene

- [ ] **Remove or update references to the deleted `restservice` module** in `copilot-instructions.md` and any README — the `GreetingController`, `Greeting`, and `GreetingControllerTest` files no longer exist
