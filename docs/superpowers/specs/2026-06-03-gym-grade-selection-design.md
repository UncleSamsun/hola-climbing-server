# 2026-06-03 Gym Grade Selection Design

## Goal

영상 등록 시 프론트가 선택한 암장의 난이도 목록을 서버에서 받아 표시하고, 사용자가 선택한 암장별 난이도를 영상에 저장한다. `gymId`와 `gymGradeId`는 영상 등록 필수값으로 전환한다.

## Pre-implementation State

- `videos.grade`는 자유 문자열이다.
- `CreateVideoRequest.grade`를 그대로 저장한다.
- 영상 등록은 `gymId`가 있으면 존재 여부만 확인하고, 없으면 암장 없는 영상 등록도 허용한다.
- `climbing_logs.grade_counts`는 JSONB이며 key가 자유 문자열이다.
- 마이그레이션 도구는 아직 도입 전이라 `schema.sql`, `seed-dev.sql`, `src/test/resources/sql/*`, `db/manual-migrations/*`를 함께 갱신해야 한다.

## Decision

`gym_grades` 마스터 테이블을 추가한다. 영상 등록은 `gymId`와 `gymGradeId`를 모두 필수로 받으며, 서버는 `gymGradeId`가 해당 `gymId`에 속한 활성 난이도인지 검증한다.

개발 단계이므로 기존 응답 호환을 위한 `videos.grade` 스냅샷은 유지하지 않는다. 새 정합성 기준은 `videos.gym_grade_id`이며, 응답은 `gymGrade` 객체(`id`, `gymId`, `label`, `difficultyOrder`)로 선택 난이도를 표현한다.

## Data Model

New table:

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

Video changes:

```sql
ALTER TABLE videos ADD COLUMN gym_grade_id BIGINT;

ALTER TABLE videos
    ADD CONSTRAINT fk_videos_gym_grade_same_gym
    FOREIGN KEY (gym_id, gym_grade_id)
    REFERENCES gym_grades(gym_id, id);

ALTER TABLE videos
    ALTER COLUMN gym_id SET NOT NULL,
    ALTER COLUMN gym_grade_id SET NOT NULL;
```

Cleanup:

- `videos.grade` is removed. Responses expose the selected grade as a `gymGrade` object.

## API Contract

### Get Gym Grades

`GET /api/gyms/{gymId}/grades`

Response:

```json
{
  "isSuccess": true,
  "code": "OK",
  "data": [
    {
      "id": 1,
      "gymId": 9001,
      "label": "빨강",
      "difficultyOrder": 10
    }
  ],
  "timestamp": "..."
}
```

Behavior:

- Returns active grades only.
- Ordered by `difficultyOrder ASC, id ASC`.
- If `gymId` does not exist or is not active, return `404 G001`.

### Create Video

`POST /api/videos`

Request changes:

```json
{
  "gymId": 9001,
  "gymGradeId": 1,
  "objectPath": "videos/uploads/10/uuid.mp4",
  "recordedDate": "2026-06-03",
  "title": "오늘의 빨강 완등",
  "isPublic": true
}
```

Rules:

- `gymId` is required.
- `gymGradeId` is required.
- `gymGradeId` must belong to `gymId`.
- `gymGradeId` must be active.
- `grade` in request is removed in favor of `gymGradeId`.
- `videos.grade` is removed; selected grade display data is returned from `gym_grades`.

Recommended error handling:

- Missing `gymId` or `gymGradeId`: `400 C001`
- Unknown/inactive gym: `404 G001`
- Grade not found, inactive, or not in gym: add `G005 INVALID_GYM_GRADE`.

## Code Touch Points

DB and seed:

- `db/schema.sql`
- `db/seed-dev.sql`
- `db/manual-migrations/YYYY-MM-DD-gym-grades.sql`
- `src/test/resources/sql/videos-schema.sql`

Gym domain:

- Add `domain/gym/domain/GymGrade.java`
- Add `domain/gym/dto/response/GymGradeResponse.java`
- Add `domain/gym/mapper/GymGradeMapper.java`
- Add `src/main/resources/mapper/gym/GymGradeMapper.xml`
- Add service method `GymService.getGrades(gymId)`
- Add controller endpoint `GET /api/gyms/{gymId}/grades`

Video domain:

- `CreateVideoRequest`: replace free `grade` input with required `gymGradeId`; make `gymId` `@NotNull`
- `Video`: add `gymGradeId`, `gymGradeLabel`, `gymGradeDifficultyOrder`
- `VideoServiceImpl.createVideo`: validate `gymId`, resolve active `GymGrade`, set `gymGradeId`
- `VideoMapper.xml`: include `gym_grade_id` in select/insert
- `VideoDetailResponse`, `VideoSummaryResponse`, `RecommendedVideoResponse`: expose `gymGrade` object; do not expose top-level `gymGradeId` or `grade`
- `RecommendationMapper.xml`: select `gym_grade_id`
- `UpdateVideoRequest` and `VideoServiceImpl.updateVideo`: remove/ignore free `grade`; do not allow grade changes in PATCH until a separate edit flow is defined.

Stats domain optional follow-up:

- Current `climbing_logs.grade_counts` remains free-key JSONB.
- Next-session implementation can leave it unchanged to keep scope focused.
- Later improvement: validate `gradeCounts` keys against `gym_grades` or change to `Map<Long, Integer>` keyed by `gymGradeId`.

## Test Plan

Add integration tests:

- `GET /api/gyms/{gymId}/grades` returns active grades ordered by difficulty.
- Unknown gym returns `404 G001`.
- `POST /api/videos` without `gymId` returns `400 C001`.
- `POST /api/videos` without `gymGradeId` returns `400 C001`.
- `POST /api/videos` with a grade from another gym returns invalid-grade error.
- `POST /api/videos` with an inactive grade returns invalid-grade error.
- Valid create stores `videos.gym_grade_id` and returns `gymGrade`.
- Existing feed/detail/recommendation/gym-video responses still work.

Recommended command:

```bash
./mvnw -Dtest=GymIntegrationTest,VideoIntegrationTest,RecommendationIntegrationTest test
```

## Notion Updates

Update API docs for:

- `영상 등록`: `gymId`, `gymGradeId`, `recordedDate`, `objectPath` required.
- `영상 상세 조회`: include `gymGrade` object; remove `grade`/top-level `gymGradeId`.
- `영상 목록 조회`: include `gymGrade` object; remove `grade`/top-level `gymGradeId`.
- `암장 영상 목록`: include `gymGrade` object; remove `grade`/top-level `gymGradeId`.
- `영상 피드 추천`: include `gymGrade` object; remove `grade`/top-level `gymGradeId`.
- Add new page or section for `GET /api/gyms/{gymId}/grades`.

## Acceptance Criteria

- Frontend can fetch a selected gym's grade list.
- Video upload cannot proceed without `gymId` and `gymGradeId`.
- Server rejects a grade that does not belong to the selected gym.
- Video responses expose enough data for frontend display without additional lookups.
- Manual migration can be applied safely to the current dev DB.
