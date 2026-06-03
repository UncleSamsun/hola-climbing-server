# Gym Grade Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add gym-specific grade masters and require `gymId` plus `gymGradeId` when creating videos.

**Architecture:** Store grade masters in `gym_grades`; expose active grades via the gym API; validate video creation against the selected gym's active grade. `videos.gym_grade_id` is the authoritative relation; the legacy `videos.grade` snapshot is removed.

**Tech Stack:** Java 25, Spring Boot 4, MyBatis XML mappers, PostgreSQL, JUnit 5, MockMvc, Testcontainers.

---

## File Map

- Create `src/main/java/com/holaclimbing/server/domain/gym/domain/GymGrade.java`: MyBatis-mapped grade row.
- Create `src/main/java/com/holaclimbing/server/domain/gym/dto/response/GymGradeResponse.java`: public grade API response.
- Create `src/main/java/com/holaclimbing/server/domain/gym/mapper/GymGradeMapper.java`: grade query interface.
- Create `src/main/resources/mapper/gym/GymGradeMapper.xml`: SQL for active grade lookup.
- Modify `src/main/java/com/holaclimbing/server/domain/gym/GymController.java`: add `GET /api/gyms/{gymId}/grades`.
- Modify `src/main/java/com/holaclimbing/server/domain/gym/service/GymService.java` and `GymServiceImpl.java`: grade list service.
- Modify `src/main/java/com/holaclimbing/server/common/exception/error/ErrorCode.java`: add `G005`.
- Modify `src/main/java/com/holaclimbing/server/domain/video/**`: require and persist `gymGradeId`.
- Modify `src/main/resources/mapper/video/VideoMapper.xml` and `src/main/resources/mapper/recommendation/RecommendationMapper.xml`: select/insert `gym_grade_id`.
- Modify `db/schema.sql`, `db/seed-dev.sql`, `src/test/resources/sql/gyms-schema.sql`, `src/test/resources/sql/gyms-data.sql`, `src/test/resources/sql/videos-schema.sql`.
- Create `db/manual-migrations/2026-06-03-gym-grades.sql`.
- Modify integration tests in `GymIntegrationTest`, `VideoIntegrationTest`, `RecommendationIntegrationTest`, `AnalysisIntegrationTest`, `NotificationIntegrationTest`.

---

### Task 1: Schema And Seed

**Files:**
- Modify: `db/schema.sql`
- Modify: `db/seed-dev.sql`
- Modify: `src/test/resources/sql/gyms-schema.sql`
- Modify: `src/test/resources/sql/gyms-data.sql`
- Modify: `src/test/resources/sql/videos-schema.sql`
- Create: `db/manual-migrations/2026-06-03-gym-grades.sql`

- [ ] **Step 1: Add schema**

Add `gym_grades` after `gyms` and before `gym_photos`:

```sql
CREATE TABLE gym_grades (
    id               BIGSERIAL PRIMARY KEY,
    gym_id           BIGINT NOT NULL REFERENCES gyms(id) ON DELETE CASCADE,
    label            VARCHAR(50) NOT NULL,
    difficulty_order INTEGER NOT NULL,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (gym_id, label),
    UNIQUE (gym_id, id)
);
CREATE INDEX idx_gym_grades_gym_order
    ON gym_grades(gym_id, difficulty_order, id)
    WHERE is_active = TRUE;
```

Add to `videos`:

```sql
gym_grade_id       BIGINT NOT NULL,
CONSTRAINT fk_videos_gym_grade_same_gym
    FOREIGN KEY (gym_id, gym_grade_id) REFERENCES gym_grades(gym_id, id),
```

- [ ] **Step 2: Add dev/test grade seed**

Use fixed ids:

```sql
INSERT INTO gym_grades (id, gym_id, label, difficulty_order, is_active) VALUES
(9001, 9001, '초록', 10, TRUE),
(9002, 9001, '파랑', 20, TRUE),
(9003, 9001, '빨강', 30, TRUE),
(9004, 9002, '노랑', 10, TRUE),
(9005, 9002, '보라', 20, TRUE),
(9006, 9002, '회색', 30, FALSE),
(9007, 9003, 'V3', 10, TRUE),
(9008, 9003, 'V4', 20, TRUE),
(9009, 9003, 'V5', 30, TRUE)
ON CONFLICT DO NOTHING;
```

- [ ] **Step 3: Backfill video seed**

For each existing seed video, set `gym_grade_id` to a grade belonging to the same `gym_id`; do not write a legacy `grade` snapshot.

- [ ] **Step 4: Write manual migration**

Create `db/manual-migrations/2026-06-03-gym-grades.sql` with idempotent `CREATE TABLE IF NOT EXISTS`, `ALTER TABLE videos ADD COLUMN IF NOT EXISTS gym_grade_id BIGINT`, fixed grade seed, legacy backfill when an old `grade` column exists, `NOT NULL`, legacy `grade` column drop, and the composite FK guarded by `pg_constraint`.

- [ ] **Step 5: Verify schema formatting**

Run:

```bash
git diff --check
```

Expected: exit 0.

---

### Task 2: Gym Grade API Tests

**Files:**
- Modify: `src/test/java/com/holaclimbing/server/domain/gym/GymIntegrationTest.java`

- [ ] **Step 1: Add failing tests**

Add:

```java
@Test
@DisplayName("암장 난이도 목록 — 활성 난이도만 난이도순으로 반환한다")
void getGymGrades_returnsActiveGradesOrdered() throws Exception {
    mockMvc.perform(get("/api/gyms/1/grades"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].label").value("초록"))
            .andExpect(jsonPath("$.data[1].label").value("파랑"))
            .andExpect(jsonPath("$.data[2].label").value("빨강"))
            .andExpect(jsonPath("$.data[2].colorHex").doesNotExist());
}

@Test
@DisplayName("암장 난이도 목록 — closed 암장은 404 G001")
void getGymGrades_closedGym_returns404() throws Exception {
    mockMvc.perform(get("/api/gyms/5/grades"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("G001"));
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```bash
./mvnw -Dtest=GymIntegrationTest#getGymGrades_returnsActiveGradesOrdered,GymIntegrationTest#getGymGrades_closedGym_returns404 test
```

Expected: fail because the route or mapper does not exist yet.

---

### Task 3: Gym Grade API Implementation

**Files:**
- Create: `src/main/java/com/holaclimbing/server/domain/gym/domain/GymGrade.java`
- Create: `src/main/java/com/holaclimbing/server/domain/gym/dto/response/GymGradeResponse.java`
- Create: `src/main/java/com/holaclimbing/server/domain/gym/mapper/GymGradeMapper.java`
- Create: `src/main/resources/mapper/gym/GymGradeMapper.xml`
- Modify: `src/main/java/com/holaclimbing/server/domain/gym/GymController.java`
- Modify: `src/main/java/com/holaclimbing/server/domain/gym/service/GymService.java`
- Modify: `src/main/java/com/holaclimbing/server/domain/gym/service/GymServiceImpl.java`

- [ ] **Step 1: Add domain and response**

`GymGrade` fields: `id`, `gymId`, `label`, `difficultyOrder`, `active`, `createdAt`, `updatedAt`.

`GymGradeResponse` fields: `id`, `gymId`, `label`, `difficultyOrder`.

- [ ] **Step 2: Add mapper**

Mapper methods:

```java
List<GymGrade> findActiveByGymId(Long gymId);
GymGrade findActiveByGymAndId(@Param("gymId") Long gymId, @Param("gradeId") Long gradeId);
```

- [ ] **Step 3: Add service/controller**

Add `List<GymGradeResponse> getGrades(Long gymId)` to `GymService`.

Implementation should call `requireGym(gymId)` and map `gradeMapper.findActiveByGymId(gymId)`.

Controller:

```java
@GetMapping("/{gymId}/grades")
public ApiResponse<List<GymGradeResponse>> getGymGrades(@PathVariable Long gymId) {
    return ApiResponse.success(gymService.getGrades(gymId));
}
```

- [ ] **Step 4: Run gym tests**

Run:

```bash
./mvnw -Dtest=GymIntegrationTest test
```

Expected: all `GymIntegrationTest` tests pass.

---

### Task 4: Video Create Validation Tests

**Files:**
- Modify: `src/test/java/com/holaclimbing/server/domain/video/VideoIntegrationTest.java`
- Modify: `src/test/java/com/holaclimbing/server/domain/recommendation/RecommendationIntegrationTest.java`
- Modify: `src/test/java/com/holaclimbing/server/domain/analysis/AnalysisIntegrationTest.java`
- Modify: `src/test/java/com/holaclimbing/server/domain/notification/NotificationIntegrationTest.java`

- [ ] **Step 1: Add failing video tests**

Add tests to `VideoIntegrationTest`:

```java
@Test
@DisplayName("영상 등록 실패 — gymId를 누락하면 400")
void createVideo_withoutGymId_returns400() throws Exception {
    String token = register("a@hola.com", "climberone");

    var body = java.util.Map.of(
            "gymGradeId", 1003,
            "objectPath", ownedObjectPath(token),
            "recordedDate", "2026-06-03",
            "isPublic", true
    );
    mockMvc.perform(post("/api/videos")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("C001"));
}

@Test
@DisplayName("영상 등록 실패 — gymGradeId를 누락하면 400")
void createVideo_withoutGymGradeId_returns400() throws Exception {
    String token = register("a@hola.com", "climberone");

    var body = java.util.Map.of(
            "gymId", 1,
            "objectPath", ownedObjectPath(token),
            "recordedDate", "2026-06-03",
            "isPublic", true
    );
    mockMvc.perform(post("/api/videos")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("C001"));
}

@Test
@DisplayName("영상 등록 실패 — 다른 암장의 난이도를 선택하면 G005")
void createVideo_gradeFromOtherGym_returnsG005() throws Exception {
    String token = register("a@hola.com", "climberone");

    var body = java.util.Map.of(
            "gymId", 1,
            "gymGradeId", 1004,
            "objectPath", ownedObjectPath(token),
            "recordedDate", "2026-06-03",
            "isPublic", true
    );
    mockMvc.perform(post("/api/videos")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("G005"));
}

@Test
@DisplayName("영상 등록 실패 — 비활성 난이도를 선택하면 G005")
void createVideo_inactiveGrade_returnsG005() throws Exception {
    String token = register("a@hola.com", "climberone");

    var body = java.util.Map.of(
            "gymId", 2,
            "gymGradeId", 1006,
            "objectPath", ownedObjectPath(token),
            "recordedDate", "2026-06-03",
            "isPublic", true
    );
    mockMvc.perform(post("/api/videos")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("G005"));
}
```

Each request body should include owned `objectPath`, required `recordedDate`, and all fields except the missing/invalid target.

- [ ] **Step 2: Update existing create helpers**

Change helper requests from the old shape `new CreateVideoRequest(null, "My Send", "a clean ascent", "V5", ownedObjectPath(token), null, 45, RECORDED_DATE, true)` to `new CreateVideoRequest(1L, "My Send", "a clean ascent", 1003L, ownedObjectPath(token), null, 45, RECORDED_DATE, true)`.

- [ ] **Step 3: Run tests and verify failure**

Run:

```bash
./mvnw -Dtest=VideoIntegrationTest test
```

Expected: fail before implementation because request/DTO and schema do not support `gymGradeId` yet.

---

### Task 5: Video Grade Implementation

**Files:**
- Modify: `src/main/java/com/holaclimbing/server/common/exception/error/ErrorCode.java`
- Modify: `src/main/java/com/holaclimbing/server/domain/video/dto/request/CreateVideoRequest.java`
- Modify: `src/main/java/com/holaclimbing/server/domain/video/dto/request/UpdateVideoRequest.java`
- Modify: `src/main/java/com/holaclimbing/server/domain/video/domain/Video.java`
- Modify: `src/main/java/com/holaclimbing/server/domain/video/service/VideoServiceImpl.java`
- Modify: `src/main/java/com/holaclimbing/server/domain/video/mapper/VideoMapper.java`
- Modify: `src/main/resources/mapper/video/VideoMapper.xml`
- Modify: `src/main/java/com/holaclimbing/server/domain/video/dto/response/VideoDetailResponse.java`
- Modify: `src/main/java/com/holaclimbing/server/domain/video/dto/response/VideoSummaryResponse.java`
- Modify: `src/main/java/com/holaclimbing/server/domain/recommendation/dto/response/RecommendedVideoResponse.java`
- Modify: `src/main/resources/mapper/recommendation/RecommendationMapper.xml`

- [ ] **Step 1: Add error code**

Add:

```java
INVALID_GYM_GRADE(HttpStatus.BAD_REQUEST, "G005", "암장 난이도가 올바르지 않습니다."),
```

- [ ] **Step 2: Change request models**

`CreateVideoRequest` should contain:

```java
@NotNull Long gymId,
@Size(max = 100) String title,
String description,
@NotNull Long gymGradeId,
@NotBlank @Size(max = 500) String objectPath,
@Size(max = 500) String thumbnailPath,
@Positive Integer durationSeconds,
@NotNull LocalDate recordedDate,
Boolean isPublic
```

`UpdateVideoRequest` should remove `grade`.

- [ ] **Step 3: Validate selected grade**

Inject `GymGradeMapper` into `VideoServiceImpl`. In `createVideo`, resolve:

```java
GymGrade grade = gymGradeMapper.findActiveByGymAndId(request.gymId(), request.gymGradeId());
if (grade == null) {
    throw new BusinessException(ErrorCode.INVALID_GYM_GRADE);
}
```

Build video with `.gymGradeId(grade.getId())`. Do not write a legacy grade label snapshot.

- [ ] **Step 4: Update SQL and responses**

Add `gym_grade_id` plus joined grade label/order to video select/insert and recommendation select. Add `gymGrade` object to detail, summary, and recommendation responses; do not expose top-level `gymGradeId` or `grade`.

- [ ] **Step 5: Run video and recommendation tests**

Run:

```bash
./mvnw -Dtest=VideoIntegrationTest,RecommendationIntegrationTest,AnalysisIntegrationTest,NotificationIntegrationTest test
```

Expected: all tests pass.

---

### Task 6: Manual DB Apply And Documentation

**Files:**
- Modify: `/Users/minjoun/Documents/DevKnowledge/10_Projects/hola-climbing-server/DB.md`
- Modify: `/Users/minjoun/Documents/DevKnowledge/50_SessionLogs/2026-06-03-hola-gym-grade-selection.md`
- Notion API pages: video create/detail/list/gym videos/recommendation and new gym grades endpoint.

- [ ] **Step 1: Apply manual migration to dev DB**

Run:

```bash
PGPASSWORD="$HOLA_DB_PASSWORD" psql -h localhost -p 5432 -U hola_user -d hola_climbing -f db/manual-migrations/2026-06-03-gym-grades.sql
```

Expected: migration completes and is idempotent.

- [ ] **Step 2: Verify dev DB**

Run:

```sql
SELECT COUNT(*) FROM gym_grades;
SELECT column_name, data_type, is_nullable FROM information_schema.columns
WHERE table_name='videos' AND column_name IN ('gym_id','gym_grade_id','grade');
```

Expected: `gym_grades` has rows, `videos.gym_grade_id` exists, and `videos.grade` is absent.

- [ ] **Step 3: Update Notion docs**

Update the pages listed in the spec so `gymId`, `gymGradeId`, `recordedDate`, and `objectPath` are required for video creation and responses include a `gymGrade` object.

- [ ] **Step 4: Final verification**

Run:

```bash
./mvnw -Dtest=GymIntegrationTest,VideoIntegrationTest,RecommendationIntegrationTest,AnalysisIntegrationTest,NotificationIntegrationTest test
git diff --check
```

Expected: Maven test build success and diff check exit 0.
