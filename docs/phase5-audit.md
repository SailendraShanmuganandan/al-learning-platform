# Phase 5 Audit: Quizzes, Question Bank, and Learning Module

**Date**: 2026-11-03  
**Auditor**: Antigravity  

---

## 1. Backend Package/Module Layout and Architecture Conventions

### 1.1 Architecture & Modular Monolith Layout
The backend is structured as a modular monolith in `com.allearning.backend`:
- **Domains/Modules**:
  - `auth`: User accounts, registration, login, verification, password resets.
  - `academic`: Subjects and chapters.
  - `catalog`: Physical notes, video products, catalog browsing.
  - `commerce`: Shopping carts, orders, order items, payments, delivery.
  - `classes`: Classes and class registrations.
  - `communication`: Announcements and contact messages.
  - `admin`: Composite admin dashboard metrics.
  - `security`: JWT filters, guards, SecurityFilterChain.
  - `common`: Global exceptions, cross-cutting models.
  - `integration`: External services (PayHere, Bunny Stream).
- **Subpackage Conventions per Domain**:
  - `controller`: REST endpoints, thin controllers delegating to service interfaces.
  - `service`: Public interfaces and implementation classes (`*ServiceImpl`), transactional boundary.
  - `repository`: Spring Data JPA repositories.
  - `entity`: JPA entity models mapped to database tables.
  - `dto`: Request and response record types.
  - `exception`: Domain-specific exceptions.
- **Cross-Module Communication Rule**:
  - Modules **must not** access repositories or entities of another module directly.
  - Cross-module interaction happens strictly through service interfaces and plain DTOs (e.g. `SubjectLookupService`, `ChapterValidationService`, `CustomerLookupService`, `StudentLookupService`, `UserLookupService`).

### 1.2 Error Handling & Formats
- Handled centrally via `GlobalExceptionHandler` (`@RestControllerAdvice`).
- Error response JSON format:
  ```json
  {
    "timestamp": "2026-11-03T20:15:00.000+05:30",
    "status": 409,
    "error": "Conflict",
    "message": "Human-readable message detail",
    "code": "ERROR_CODE",
    "fields": { "field": "validation error message" }
  }
  ```
- Error codes in use: `EMAIL_NOT_VERIFIED`, `CART_CONFLICT`, `REGISTRATION_CONFLICT`, `INVALID_TRANSITION`, `RATE_LIMIT_EXCEEDED`, `NOT_ENOUGH_QUESTIONS`, etc.

### 1.3 Authentication, Current User, and Security
- Authentication is stateless JWT (`Bearer <token>`).
- Filter: `JwtAuthenticationFilter` sets `UsernamePasswordAuthenticationToken` in `SecurityContextHolder` with `ROLE_<ROLE>`.
- Admin routes:
  - Configured in `SecurityConfig`: `.requestMatchers("/api/admin/**").hasRole("ADMIN")`.
  - Unauthenticated requests yield `401 Unauthorized` via `AuthenticationEntryPoint`.
  - Non-admin authenticated users (e.g., `ROLE_STUDENT`) yield `403 Forbidden`.
  - Controllers can also use `@PreAuthorize("hasRole('ADMIN')")`.
- Obtaining current user in cross-module code:
  - Controllers receive `Authentication authentication`.
  - Calling `authentication.getName()` returns the authenticated user's email.
  - The consuming module uses its domain lookup service interface (implemented in `auth`) to resolve email to `userId` cleanly without entity leakage.

### 1.4 Pagination Conventions
- Handled via Spring Data Web support: `@PageableDefault(size = 20)` with `@SortDefault`.
- Handled either via Spring Data `Page<T>` serialization or custom page wrapper DTOs.

### 1.5 Configuration Properties & Schedulers
- Config properties use `@ConfigurationProperties(prefix = "...")` or `@Value("${...:default}")`.
- Scheduling is enabled with `@EnableScheduling` and methods annotated with `@Scheduled`.

### 1.6 Testing Conventions
- **No Testcontainers**: Tests run against an in-memory H2 database configured in PostgreSQL compatibility mode:
  ```properties
  spring.datasource.url=jdbc:h2:mem:...;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=false
  spring.datasource.driver-class-name=org.h2.Driver
  spring.jpa.hibernate.ddl-auto=create-drop
  spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
  spring.flyway.enabled=false
  ```
- Integration tests use `@SpringBootTest` with `MockMvc` and Spring Security filter chain.
- Unit tests use Mockito with `@ExtendWith(MockitoExtension.class)`.

---

## 2. Existing Flyway Migrations & Schema State

- Current latest migration: `V15__admin_dashboard_indexes.sql`.
- Next migration version: **`V16`**.
- Existing tables:
  1. `users` (V1)
  2. `email_verification_tokens` (V2)
  3. `password_reset_tokens` (V3)
  4. `addresses` (V4)
  5. `subjects` (V5)
  6. `chapters` (V6)
  7. `products`, `notes`, `videos`, `product_chapters` (V7, V12)
  8. `carts`, `cart_items` (V8)
  9. `orders`, `order_items` (V9, V11)
  10. `payments`, `payment_notifications` (V10, V11)
  11. `classes`, `class_registrations` (V13)
  12. `announcements`, `contact_messages` (V14)

---

## 3. Existing Quiz / Assessment Code & Data

- An empty package `com.allearning.backend.quiz` exists containing only `package-info.java`.
- No quiz-related tables exist in the database or Flyway migrations.
- Frontend contains only text placeholder references in `ChapterDetailPage.tsx` and `AdminLayout.tsx`.
- **Verdict**: No existing quiz data or schema conflicts exist. We will remove the placeholder `quiz` package and create the new domain module **`learning`** (`com.allearning.backend.learning`).

---

## 4. Academic Module Chapter/Subject Service Exposure

- Academic exposes:
  - `ChapterValidationService`:
    - `doAllChaptersExist(Collection<Long>)`
    - `findMissingChapterIds(Collection<Long>)`
    - `getPublicChapterSummaries(Collection<Long>)`
    - `getPublicChapterIdsForSubject(Long)`
    - `isPubliclyVisible(Long)`
  - `SubjectLookupService`:
    - `getSubjectInfo(Long)`
    - `isSubjectActive(Long)`
    - `getActiveSubjectIds()`
    - `getSubjectsInfo(Collection<Long>)`
- In `Chapter.java`, chapters have `status` (`ACTIVE` / `INACTIVE`) and link to `Subject` (`status`: `ACTIVE` / `INACTIVE`).
- A chapter is "active" when **both** the chapter and its subject have `status = 'ACTIVE'`.
- We will expand `ChapterValidationService` / `AcademicService` to provide:
  - `isChapterActive(Long chapterId)`
  - `getChapterSummary(Long chapterId)` returning `(id, name, subjectId, active)`
  - `getActiveChapterIds()`
  - `getActiveChapterIdsBySubject(Long subjectId)`
  - `getChapterNames(Collection<Long> chapterIds)`

---

## 5. Conclusion & Action Plan
All prerequisites are clear and no unresolvable conflicts exist.
Proceeding with:
1. Academic service extension for chapter status and lookups.
2. Flyway migration `V16__create_learning_tables.sql` containing all 14 tables and indexes.
3. JPA entities and repositories for `learning` module.
4. `LearningProperties` configuration properties class (`app.learning.*`).
5. Chapter Topics admin endpoints (`FR-TOPIC-01..03`).
6. Quiz and Question Bank admin endpoints (`FR-QUIZ-12`, `FR-QBANK-01..12`, `BR-34`, `BR-35`, `BR-38`).
7. Eligibility & Sampling services (`QuestionEligibility`, `QuestionSamplingService`).
8. Unit and integration tests.

