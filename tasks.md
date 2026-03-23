# Implementation Tasks

User stories ordered by dependency — each story assumes all preceding stories are complete.
Stories are written as vertical slices: each one delivers a working, testable increment.

---

## Story 1 — User registration and profile ✅ DONE

> **As a new user, I want to register an account and view my own profile,
> so that I have an identity in the system.**

### Scope

- New `user` module (`nl.kabisa.dashboarding.user`)
- `User` entity: `id` (UUID), `username` (unique), `email` (unique), `passwordHash`, `createdAt`, `modifiedAt`
- `UserRepository` with `findByUsername` and `findByEmail`
- `UserService` with `register(username, email, password)` — hashes password with BCrypt before storing
- `POST /users` — register; returns `201 Created` with `{ id, username, email, createdAt }`
- `GET /users/me` — returns the profile of the currently authenticated user (stub: always returns the single user for now; real auth wired in Story 2)
- `GlobalExceptionHandler` entries: duplicate username/email → `409 Conflict`
- Integration tests: register succeeds, duplicate username rejected, duplicate email rejected, get profile

### Does not include

- Authentication (Story 2)
- Ownership on Widget or Dashboard (Story 4 / Story 5)

### Implementation notes

- `PasswordEncoder` exposed as a `@Bean` via `user/PasswordEncoderConfig.java` — reuse or move to `SecurityConfig` in Story 2
- `GlobalExceptionHandler` moved to root package `nl.kabisa.dashboarding` (shared across all modules)
- `ValidationErrorResponse` also moved to root package
- `GET /users/me` identifies the caller via `X-User-Id` header (UUID) — **swap to JWT subject extraction in Story 2**
- `spring.jackson.datatype.datetime.write-dates-as-timestamps=false` added to `application.properties` (Jackson 3 / Spring Boot 4 syntax — differs from Jackson 2's `spring.jackson.serialization.*`)

---

## Story 2 — Authentication ✅ DONE

> **As a registered user, I want to log in and receive a token,
> so that I can make authenticated API calls.**

### Dependencies

- Story 1 (User entity)

### Scope

- Add `spring-boot-starter-security` to `pom.xml`
- Implement `SecurityConfig`:
  - Public: `POST /users` (register), `POST /auth/login`
  - Protected: everything else
- `POST /auth/login` — accepts `{ username, password }`; validates credentials; returns a signed JWT
- JWT filter: validate `Authorization: Bearer <token>` on every protected request
- `GET /users/me` now returns the profile of the authenticated caller from the JWT subject
- `401 Unauthorized` for missing or invalid token on protected endpoints
- `403 Forbidden` for valid token but insufficient permission (framework-level; permission rules added in later stories)
- Update all existing integration tests to supply a valid JWT (or use a test helper that bypasses security)

### Does not include

- CORS (Story 3)
- Ownership enforcement (Story 4)

### Implementation notes (carry-over from Story 1)

- **`PasswordEncoder` bean**: moved into `SecurityConfig` (the `user/PasswordEncoderConfig.java` was deleted)
- **`GET /users/me` stub**: replaced with `Authentication` principal from `SecurityContextHolder` — `X-User-Id` header logic removed
- **Test helper**: `JwtTestHelper` added in `src/test/java/nl/kabisa/dashboarding/auth/` — mints test tokens; all existing integration tests updated to use it
- **Additional components**: `AuthEntryPoint` (401 responses), `AuthAccessDeniedHandler` (403 responses), `JwtAuthenticationFilter`, `JwtService`, `JwtProperties`, `UserDetailsServiceImpl`

---

## Story 2b — User roles and approval

> **As an administrator, I want newly registered users to require my approval before they can use the system,
> so that I control who has access.**

### Dependencies

- Story 2 (Authentication — JWT and Spring Security must be in place)

### Scope

- Add `role` enum (`USER`, `ADMIN`) to `User` entity (default: `USER`)
- Add `enabled` (boolean) to `User` entity (default: `false`) — unapproved users cannot authenticate
- `POST /users` (register) — remains public; newly registered users are created with `role = USER` and `enabled = false`
- `POST /auth/login` — reject login for disabled users with `403 Forbidden` and a clear message (e.g. "Account pending approval")
- **Admin user seeding**: on application startup, if no admin exists, create one from config properties `dashboarding.admin.username`, `dashboarding.admin.email`, `dashboarding.admin.password`; this user gets `role = ADMIN` and `enabled = true`
- `GET /users` — list all users with their `role` and `enabled` status; `ADMIN` only
- `PUT /users/{id}/approve` — set `enabled = true`; `ADMIN` only; returns updated user profile; `404` if user not found
- `PUT /users/{id}/role` — change a user's role (body: `{ "role": "ADMIN" | "USER" }`); `ADMIN` only; an admin cannot demote themselves
- `UserDetailsServiceImpl` — read `role` from the `User` entity and map to `ROLE_USER` / `ROLE_ADMIN` granted authority (replace hardcoded `ROLE_USER`)
- `JwtAuthenticationFilter` — include the role from the database rather than hardcoding `ROLE_USER`
- `SecurityConfig` — add role-based access rules: `GET /users`, `PUT /users/{id}/approve`, and `PUT /users/{id}/role` require `ROLE_ADMIN`
- `GET /users/me` — add `role` and `enabled` fields to `UserProfileResponse`
- Integration tests: register → user is disabled → login rejected; admin approves → login succeeds; admin can list users; admin can change roles; non-admin cannot access admin endpoints (`403`); seeded admin exists on startup

### Does not include

- Group membership roles (Story 9 — `MEMBER` / `ADMIN` within a group is a separate concept)
- Password reset or email verification

### Implementation notes

- The `enabled` field maps naturally to Spring Security's `UserDetails.isEnabled()` — `DaoAuthenticationProvider` will reject disabled accounts automatically if wired correctly
- The seeded admin should be created via an `ApplicationRunner` or `@PostConstruct` bean that checks on every startup but only inserts if no `ADMIN`-role user exists (idempotent)
- Consider adding `role` as a claim in the JWT so the filter does not need a DB lookup per request — but be aware this means role changes only take effect after re-login

---

## Story 3 — CORS ✅ DONE

> **As a frontend developer, I want the API to accept cross-origin requests from the React app,
> so that the browser does not block my API calls.**

### Dependencies

- Story 2 (Spring Security must be in place — CORS must be configured through it, not around it)

### Scope

- Add `dashboarding.cors.allowed-origins` to `application.properties` (default: `http://localhost:3000`)
- Register a `CorsConfigurationSource` bean inside `SecurityConfig` wired to the above property
- Allow `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS` with `Authorization` and `Content-Type` headers
- Integration test: preflight `OPTIONS` request returns `200` with correct CORS headers

### Does not include

- Any domain changes

---

## Story 4 — Widget ownership ✅ DONE

> **As an authenticated user, I want every widget I create to be mine,
> so that only I can modify or delete it.**

### Dependencies

- Story 2 (authenticated caller available in request context)

### Scope

- Add `owner` (`@ManyToOne(EAGER) User`, `NOT NULL`) FK to `Widget` entity
- `WidgetService.createWidget` — sets `owner` from the authenticated principal; returns `404` if the caller's user record is not found
- All read and write operations (`getWidget`, `getWidgetWithChildren`, `getChildren`, `updateWidget`, `deleteWidgetWithDescendants`, `getWidgetEndpoint`) — reject with `403` if the caller is not the owner; `404` always precedes `403` (widget is looked up first, then ownership is asserted)
- Access control is JWT-stateless: deleted users retain access to their widgets until their token expires
- Add `ownerId` and `ownerName` to `GetWidgetResponse` and `WidgetChildSummary`
- `GET /widget/{id}/children` returns all direct children regardless of each child's owner (parent-owner sees all children); child visibility for non-owners will be revisited in Story 10
- Integration tests: owner can read, update, and delete their widget; another user is rejected with `403`; non-existent widget returns `404` (not `403`); `ownerId`/`ownerName` appear in GET and children responses

### Does not include

- Group sharing (Story 10)
- Widget list endpoint (Story 11)

---

## Story 5 — Dashboard CRUD

> **As an authenticated user, I want to create, view, update, and delete my own dashboards,
> so that I can manage my personal dashboard collection.**

### Dependencies

- Story 2 (authenticated caller)

### Scope

- Add nullable `owner` (`@ManyToOne User`) FK to `Dashboard` entity
- Add `DashboardService` — move business logic out of `DashboardController` (currently calls repository directly)
- `POST /dashboards` — create dashboard (`name`); set owner from principal; return `{ id, name, createdAt }`
- `GET /dashboards/{id}` — return a single dashboard by id; `404` if not found or soft-deleted
- `PUT /dashboards/{id}` — rename or update config; owner only; `403` for non-owners
- `DELETE /dashboards/{id}` — soft-delete (`deletedAt = now()`); owner only; `403` for non-owners
- Existing `GET /dashboards` continues to list only non-deleted dashboards owned by the authenticated user
- Integration tests: full lifecycle (create → get → update → delete → confirm gone); `403` for wrong owner

### Does not include

- Linking dashboards to a widget tree (Story 6)
- Shareable URLs (Story 12)

---

## Story 6 — Dashboard as widget (unified model)

> **As a user, I want my dashboard to have a widget tree,
> so that I can nest grids, slideshows, and leaf widgets inside it.**

### Dependencies

- Story 4 (Widget ownership)
- Story 5 (Dashboard CRUD and service layer)

### Scope

- Add nullable `rootWidget` (`@ManyToOne Widget`) FK on `Dashboard` entity (`rootWidgetId`)
- `POST /dashboards` — optionally accept a `rootWidgetId`; if omitted, auto-create a root widget of `widgetType = "dashboard"` owned by the same user
- `GET /dashboards/{id}` — include the `rootWidgetId` in the response; optionally accept `?expand=tree` query param to return the full nested widget tree (recursive child expansion)
- `DELETE /dashboards/{id}` — cascade soft-delete the entire widget tree rooted at `rootWidgetId`
- Integration tests: create dashboard → verify root widget exists; delete dashboard → verify widget tree is soft-deleted; expand tree returns nested structure

### Does not include

- Sharing (Story 10)

---

## Story 7 — Widget full update

> **As the owner of a widget, I want to update its configuration, model, and data endpoints,
> so that I can change its settings after initial creation.**

### Dependencies

- Story 4 (ownership check in place)

### Scope

- Expand `UpdateWidgetRequest` to accept optional fields: `widgetType`, `version`, `configuration`, `configurationModel`, `dataEndpoints`
- `WidgetService.updateWidget` — for each provided field, re-run `ConfigurationValidator` and `ConfigurationExtractor`; persist updated frontend config, secrets config, model, and endpoints
- Ownership check already in place from Story 4 — no duplication needed
- Integration tests: owner updates configuration → GET reflects new values; invalid config rejected with `400`; non-owner rejected with `403`

### Does not include

- Cache invalidation on update (Story 14)

---

## Story 8 — Widget soft-delete and cleanup

> **As an operator, I want deleted widgets to be retained for a configurable period before permanent removal,
> so that accidental deletions can be recovered and foreign-key integrity is maintained.**

### Dependencies

- Story 4 (ownership — must know who owns what before safely soft-deleting)
- Story 6 (dashboard cascade soft-delete relies on this)

### Scope

- Add `deletedAt` (nullable timestamp) to `Widget` entity
- Change `WidgetService.deleteWidgetWithDescendants` — set `deletedAt = now()` on the widget and all descendants instead of hard-deleting
- Update all repository queries to add `WHERE deleted_at IS NULL` guard: `findByParentId`, `findChildIdsByParentId`, `findAllDescendantIds`, `findAllAncestorIds`, `deleteWidgetTree` (rename to `softDeleteWidgetTree`)
- Add `dashboarding.widget.retention-days` config property (default: `30`)
- Add `@Scheduled` cleanup job in `WidgetService` — hard-deletes widgets and dashboards whose `deletedAt` is older than the retention period
- Integration tests: deleted widget is excluded from all queries; cleanup job hard-deletes records beyond retention threshold

### Does not include

- Sharing guard on delete (Story 10 — share check added there)

---

## Story 9 — Groups and membership

> **As a user, I want to create groups and add other users as members,
> so that I can organise users for widget sharing.**

### Dependencies

- Story 2 (authenticated caller)

### Scope

- New `group` module (`nl.kabisa.dashboarding.group`)
- `Group` entity: `id` (UUID), `name`, `createdAt`
- `UserGroup` join entity: `userId`, `groupId`, `role` (`MEMBER` / `ADMIN`)
- `GroupRepository` and `GroupService`
- `POST /groups` — create group; creator automatically added as `ADMIN`
- `GET /groups` — list all groups (admin use); `GET /groups/me` — list groups the authenticated user belongs to
- `GET /groups/{id}` — get group with member list (members of the group only)
- `POST /groups/{id}/members` — add a user by username; `ADMIN` only; `403` for non-admins
- `DELETE /groups/{id}/members/{userId}` — remove a member; `ADMIN` only
- Integration tests: create group, add member, list my groups, non-admin cannot add members

### Does not include

- Widget sharing (Story 10)

---

## Story 10 — Widget sharing and permission enforcement

> **As a widget owner, I want to share my widget with a group,
> so that group members can use it in their dashboards without being able to change it.**

### Dependencies

- Story 4 (widget ownership)
- Story 8 (soft-delete guard must be in place — sharing blocks deletion)
- Story 9 (groups exist)

### Scope

- `WidgetShare` entity: `widgetId`, `groupId`, `permission` (`READ_ONLY` / `READ_WRITE`)
- `POST /widget/{id}/share` — share with a group; body: `{ groupId, permission }`; owner only; `404` if group not found
- `DELETE /widget/{id}/share/{groupId}` — revoke a share; owner only
- `GET /widget/{id}/shares` — list active shares for a widget; owner only
- **Deletion guard**: `WidgetService.deleteWidgetWithDescendants` — check for active `WidgetShare` entries before soft-deleting; reject with `409 Conflict` if any exist
- **Read-only enforcement**: `WidgetService.updateWidget` — if caller is a group member (not owner), reject `PUT` with `403`
- `GetWidgetResponse` — add `shares: [{ groupId, groupName, permission }]` (visible to owner only; omitted for non-owners)
- **`GET /widget/{id}/children`**: currently returns all direct children regardless of who owns each child (the parent owner sees children owned by others). When sharing is introduced, revisit this endpoint — a shared-read caller should only see children they also have access to, and the ownership / permission model for child visibility needs to be explicitly defined.
- Integration tests: owner shares → group member can GET but not PUT or DELETE; owner revokes share → member loses access; owner cannot delete while shares are active

### Does not include

- Widget list filtered by shares (Story 11)

---

## Story 11 — Widget discovery (list endpoint)

> **As an authenticated user, I want to browse widgets I own and widgets shared with my groups,
> so that I can find and reuse existing widgets when building a dashboard.**

### Dependencies

- Story 4 (ownership)
- Story 10 (sharing — shared widgets must appear in the list)

### Scope

- `GET /widgets` — returns widgets owned by the authenticated user plus widgets shared with any of the user's groups
- Query parameters: `widgetType` (filter by type), `rootOnly=true` (only widgets without a parent)
- Response: list of `WidgetSummary` (`id`, `widgetType`, `version`, `ownerId`, `ownerName`, `parentId`, `permission`); `permission` is `OWNER`, `READ_WRITE`, or `READ_ONLY`
- `WidgetRepository` — add query combining owned + shared widgets (union or JOIN through `WidgetShare` + `UserGroup`)
- Integration tests: user sees own widgets; user sees group-shared widgets; filter by `widgetType`; `rootOnly` excludes children; user does not see unshared widgets of other users

---

## Story 12 — Shareable dashboard URLs

> **As a dashboard owner, I want to generate a shareable link for my dashboard,
> so that I can give others read-only access without requiring an account.**

### Dependencies

- Story 5 (Dashboard CRUD — dashboard must have full lifecycle before adding share tokens)

### Scope

- Add nullable `shareToken` (UUID) to `Dashboard` entity
- `POST /dashboards/{id}/share` — generate and persist a random `shareToken`; return `{ shareUrl }`; owner only
- `DELETE /dashboards/{id}/share` — set `shareToken = null`; owner only
- `GET /dashboards/public/{token}` — resolve token; return dashboard with widget tree; no authentication required; `404` if token not found or dashboard is soft-deleted
- `DashboardRepository.findByShareToken`
- Integration tests: generate token → public URL returns dashboard; revoke token → public URL returns `404`; unauthenticated caller can access public URL

### Does not include

- Password protection (Story 13)

---

## Story 13 — Password-protected public dashboards

> **As a dashboard owner, I want to optionally protect my shared dashboard with a password,
> so that only people I give the password to can view it.**

### Dependencies

- Story 12 (share token mechanism must exist)

### Scope

- Add `passwordHash` (nullable String) to `Dashboard` entity
- `PUT /dashboards/{id}/password` — set or replace the password (BCrypt-hashed before storage); owner only
- `DELETE /dashboards/{id}/password` — remove password requirement; owner only
- `GET /dashboards/public/{token}` — if `passwordHash` is set, require `X-Dashboard-Password` header; return `401` on mismatch or missing header
- Integration tests: password-protected dashboard returns `401` without header; correct password grants access; wrong password returns `401`; removing password makes dashboard freely accessible

---

## Story 14 — Widget data endpoint caching

> **As a user viewing a dashboard, I want widget data to be cached on the server,
> so that repeated requests do not hammer external services.**

### Dependencies

- Story 7 (widget full update — cache must be invalidated when configuration changes)

### Scope

- Add `spring-boot-starter-cache` + Caffeine to `pom.xml`
- Enable `@EnableCaching` in the application
- `WidgetEndpointsController.getWidgetEndpoint` — wrap step execution with a cache check; cache key: `widgetId + ":" + endpointName`; TTL from `DataEndpointModelItem.cache` (milliseconds); skip caching if `cache = 0`
- `WidgetService.updateWidget` — evict all cache entries for the widget after a successful update
- Integration tests with WireMock: first call hits external service; second call within TTL does not; cache is evicted after widget update

### Does not include

- Rate limiting (Story 15)

---

## Story 15 — Widget data endpoint rate limiting

> **As an operator, I want to limit how often a widget's data endpoint can be called,
> so that misconfigured or malicious widgets cannot overwhelm external services.**

### Dependencies

- Story 14 (caching infrastructure in place — rate limiting shares the same request interception point)

### Scope

- Add `rateLimit` field (integer, requests per minute; `0` = unlimited) to `DataEndpointModelItem`
- Add Bucket4j (+ Caffeine backend) to `pom.xml`
- `WidgetEndpointsController.getWidgetEndpoint` — check rate limit bucket before executing steps; return `429 Too Many Requests` with `Retry-After` header when exceeded
- Bucket key: `widgetId + ":" + endpointName`
- Integration tests: requests within limit succeed; requests exceeding limit return `429`; `rateLimit = 0` imposes no limit

---

## Story 16 — Secrets encryption hardening

> **As an operator, I want widget secrets to be encrypted with modern cryptography,
> so that a database breach does not expose credentials in plaintext.**

### Dependencies

- Story 7 (widget full update — hardened encryption applies to both create and update paths)

### Scope

- Replace `EncryptionUtil` implementation: AES/GCM/NoPadding with a random 96-bit IV per operation
- Remove the hardcoded `"widget-encryption-salt"` string; derive key from a configurable server-side master secret (`dashboarding.encryption.master-key` in `application.properties` / env var)
- Store `IV + ciphertext` (Base64-encoded, colon-separated) in `secretsConfiguration`
- Wire up `ConfigurationFieldStorage` enum on `ConfigurationModelItem` — honour `PLAIN` vs `ENCRYPTED` storage per field; `scope = backend` no longer implies encryption by itself
- Migration path: on first read of a widget with the old format, re-encrypt and persist with the new format (detect by absence of the IV prefix)
- Integration tests: encrypted value round-trips correctly; changing master key requires explicit re-encryption (not silent breakage); `PLAIN` backend fields are stored unencrypted

---

## Story 17 — Database migrations (Flyway)

> **As an operator, I want database schema changes to be versioned and applied automatically,
> so that deployments are safe, repeatable, and auditable.**

### Dependencies

- Story 16 (last story that changes the data model — baseline migration written after the schema stabilises)

### Scope

- Add `flyway-core` to `pom.xml`
- Set `spring.jpa.hibernate.ddl-auto=validate` in `application.properties`
- Write `V1__baseline.sql` — captures the full schema as it stands after Story 16 (widgets, dashboards, users, groups, user_groups, widget_shares tables with all columns and FK constraints)
- Write incremental migration scripts for any schema adjustments needed to bring the current `ddl-auto=update` state in line with the validated baseline
- Update Docker Compose / CI setup so Flyway runs before the application starts
- Integration tests continue to pass with `ddl-auto=validate`

---

## Dependency map

```
Story 1 (Users ✅)
  └─ Story 2 (Auth ✅)
       ├─ Story 2b (User roles & approval)
       ├─ Story 3 (CORS ✅)
       ├─ Story 4 (Widget ownership ✅)
       │    ├─ Story 7 (Widget full update)
       │    │    ├─ Story 14 (Caching)
       │    │    │    └─ Story 15 (Rate limiting)
       │    │    └─ Story 16 (Encryption hardening)
       │    │         └─ Story 17 (Flyway migrations)
       │    └─ Story 8 (Soft-delete)
       │         └─ Story 10 (Sharing & permissions)
       │              └─ Story 11 (Widget list)
       ├─ Story 5 (Dashboard CRUD)
       │    ├─ Story 6 (Dashboard as widget)
       │    └─ Story 12 (Shareable URLs)
       │         └─ Story 13 (Password protection)
       └─ Story 9 (Groups)
            └─ Story 10 (Sharing & permissions)
```
