# Recommendation Feed Snapshot And Seen Penalty Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Redis-backed recommendation feed snapshots so cursor pages reuse a stable ranked list, and add user-video interaction history so already viewed or exposed videos are scored lower.

**Architecture:** First-page feed requests compute a ranked ID list, apply viewed/impressed penalties, store the list in Redis with a short TTL, return the first page, and encode a snapshot cursor containing `snapshotId + offset`. Cursor requests read the snapshot slice instead of recomputing the ranking window. User-video interactions are stored in PostgreSQL and used in the first-page candidate ranking; strict visibility filtering remains request-time.

**Tech Stack:** Spring Boot 4, Java 25, MyBatis XML mappers, PostgreSQL/Flyway, Redis `StringRedisTemplate`, Jackson, JUnit/MockMvc/Testcontainers, k6 performance evidence.

---

## Scope

Implement only the API-side snapshot path and viewed/impressed score penalty.

In scope:

- New PostgreSQL table for user-video feed interactions.
- Record feed impressions when a recommendation page is returned.
- Record actual video views when video detail is opened.
- Penalize recently viewed/impressed videos in recommendation ranking.
- Store first-page ranked IDs as a Redis snapshot.
- Use `snapshotId + offset` cursor for next pages.
- Keep strict filters for blocked/deleted/private/self videos at response time.
- Update tests and performance evidence scripts enough to measure first-page snapshot generation and cursor-page snapshot retrieval.

Out of scope:

- Worker precompute.
- Scheduler warm-up.
- Client-side play-progress events.
- Full recommendation model redesign.
- Cloud deployment changes.

## Design Decisions

- Snapshot TTL: `10 minutes`.
- Snapshot size: `1,000` ranked video IDs.
- Candidate window remains `max(5000, snapshotSize * 2)` for first implementation; tune after measurements.
- Viewed penalty is stronger than feed-impression penalty.
- Impression means "returned in recommendation feed response", not "actually watched".
- Actual view means `GET /api/videos/{id}` detail endpoint was opened.
- Cursor snapshot expiration returns `INVALID_INPUT` with a clear message asking the client to refresh the first page.
- Current keyset cursor support can be removed or retained only for backward compatibility; prefer snapshot cursor for new responses.

## File Structure

Create:

- `src/main/resources/db/migration/V8__recommendation_feed_interactions.sql`
  - Creates `user_video_interactions` with per-user/per-video impression and view metadata.
- `src/main/java/com/holaclimbing/server/domain/recommendation/domain/RecommendationFeedSnapshot.java`
  - Serializable snapshot payload stored in Redis.
- `src/main/java/com/holaclimbing/server/domain/recommendation/service/RecommendationFeedSnapshotStore.java`
  - Redis read/write abstraction for snapshots.
- `src/main/java/com/holaclimbing/server/domain/recommendation/mapper/RecommendationInteractionMapper.java`
  - MyBatis mapper for impression/view upserts.
- `src/main/resources/mapper/recommendation/RecommendationInteractionMapper.xml`
  - SQL upserts for `user_video_interactions`.

Modify:

- `src/main/java/com/holaclimbing/server/domain/recommendation/dto/RecommendationCursor.java`
  - Represent snapshot cursor fields: `snapshotId`, `offset`.
- `src/main/java/com/holaclimbing/server/domain/recommendation/dto/RecommendationCursorCodec.java`
  - Encode/decode versioned snapshot cursor payload.
- `src/main/java/com/holaclimbing/server/domain/recommendation/mapper/RecommendationMapper.java`
  - Add candidate snapshot query and ID lookup query.
- `src/main/resources/mapper/recommendation/RecommendationMapper.xml`
  - Add seen penalty ranking and snapshot ID lookup.
- `src/main/java/com/holaclimbing/server/domain/recommendation/service/RecommendationServiceImpl.java`
  - Split first-page snapshot generation from cursor-page snapshot reading.
- `src/main/java/com/holaclimbing/server/domain/video/service/VideoServiceImpl.java`
  - Record actual views in `user_video_interactions` when `viewerId != null`.
- `perf/sql/recommendation_feed_explain_text.sql`
  - Rename/report first-page snapshot candidate EXPLAIN.
- `perf/sql/recommendation_feed_explain_json.sql`
  - Same as text report for JSON.
- `perf/scripts/report_recommendation_sql.sh`
  - Record `snapshot_size`.
- `perf/scripts/render_recommendation_presentation.py`
  - Add snapshot/seen fields to presentation cards after evidence is available.

Test:

- `src/test/java/com/holaclimbing/server/domain/recommendation/service/RecommendationServiceImplTest.java`
- `src/test/java/com/holaclimbing/server/domain/recommendation/RecommendationIntegrationTest.java`
- `src/test/java/com/holaclimbing/server/domain/video/VideoIntegrationTest.java`

---

### Task 1: Add User-Video Interaction Storage

**Files:**

- Create: `src/main/resources/db/migration/V8__recommendation_feed_interactions.sql`
- Create: `src/main/java/com/holaclimbing/server/domain/recommendation/mapper/RecommendationInteractionMapper.java`
- Create: `src/main/resources/mapper/recommendation/RecommendationInteractionMapper.xml`
- Test: `src/test/java/com/holaclimbing/server/domain/recommendation/RecommendationIntegrationTest.java`

- [ ] **Step 1: Write integration test for impression penalty data**

Add a test that seeds three public videos for one viewer:

1. unseen video
2. impressed video
3. viewed video

The expected order after recommendation ranking should place unseen before impressed and viewed when all other ranking signals are equivalent.

Suggested test name:

```java
@Test
@DisplayName("추천 피드 - 최근 노출/조회된 영상은 같은 점수 후보보다 뒤로 밀린다")
void recommendationFeedPenalizesRecentlySeenVideos() throws Exception {
    // Arrange: create viewer and three videos with same gym/style signal.
    // Insert user_video_interactions rows for impressed/viewed videos.
    // Act: GET /api/recommendations/videos?size=3
    // Assert: unseen video id appears before impressed/viewed ids.
}
```

Run:

```bash
./mvnw -Dtest=RecommendationIntegrationTest#recommendationFeedPenalizesRecentlySeenVideos test
```

Expected before implementation: migration/table or mapper behavior is missing, so the test fails.

- [ ] **Step 2: Add Flyway migration**

Create:

```sql
CREATE TABLE user_video_interactions (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    video_id BIGINT NOT NULL REFERENCES videos(id) ON DELETE CASCADE,
    impression_count INTEGER NOT NULL DEFAULT 0,
    viewed_count INTEGER NOT NULL DEFAULT 0,
    last_impressed_at TIMESTAMPTZ,
    last_viewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, video_id)
);

CREATE INDEX idx_user_video_interactions_user_impressed
    ON user_video_interactions(user_id, last_impressed_at DESC)
    WHERE last_impressed_at IS NOT NULL;

CREATE INDEX idx_user_video_interactions_user_viewed
    ON user_video_interactions(user_id, last_viewed_at DESC)
    WHERE last_viewed_at IS NOT NULL;
```

- [ ] **Step 3: Add mapper interface**

Create:

```java
package com.holaclimbing.server.domain.recommendation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecommendationInteractionMapper {

    int upsertImpressions(@Param("userId") Long userId,
                          @Param("videoIds") List<Long> videoIds);

    int upsertView(@Param("userId") Long userId,
                   @Param("videoId") Long videoId);
}
```

- [ ] **Step 4: Add mapper XML**

Create:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.holaclimbing.server.domain.recommendation.mapper.RecommendationInteractionMapper">

    <insert id="upsertImpressions">
        INSERT INTO user_video_interactions (user_id, video_id, impression_count, last_impressed_at, updated_at)
        VALUES
        <foreach collection="videoIds" item="videoId" separator=",">
            (#{userId}, #{videoId}, 1, NOW(), NOW())
        </foreach>
        ON CONFLICT (user_id, video_id)
        DO UPDATE SET
            impression_count = user_video_interactions.impression_count + 1,
            last_impressed_at = NOW(),
            updated_at = NOW()
    </insert>

    <insert id="upsertView">
        INSERT INTO user_video_interactions (user_id, video_id, viewed_count, last_viewed_at, updated_at)
        VALUES (#{userId}, #{videoId}, 1, NOW(), NOW())
        ON CONFLICT (user_id, video_id)
        DO UPDATE SET
            viewed_count = user_video_interactions.viewed_count + 1,
            last_viewed_at = NOW(),
            updated_at = NOW()
    </insert>

</mapper>
```

- [ ] **Step 5: Run migration-aware test**

Run:

```bash
./mvnw -Dtest=RecommendationIntegrationTest#recommendationFeedPenalizesRecentlySeenVideos test
```

Expected after storage only: table/mappers load, but ranking still fails until Task 2.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/db/migration/V8__recommendation_feed_interactions.sql \
        src/main/java/com/holaclimbing/server/domain/recommendation/mapper/RecommendationInteractionMapper.java \
        src/main/resources/mapper/recommendation/RecommendationInteractionMapper.xml \
        src/test/java/com/holaclimbing/server/domain/recommendation/RecommendationIntegrationTest.java
git commit -m "feat(recommendation): store user video feed interactions"
```

---

### Task 2: Apply Seen Penalty In Recommendation Ranking

**Files:**

- Modify: `src/main/java/com/holaclimbing/server/domain/recommendation/mapper/RecommendationMapper.java`
- Modify: `src/main/resources/mapper/recommendation/RecommendationMapper.xml`
- Test: `src/test/java/com/holaclimbing/server/domain/recommendation/RecommendationIntegrationTest.java`

- [ ] **Step 1: Add mapper method for snapshot candidates**

Add:

```java
List<Video> findFeedSnapshotCandidates(@Param("userId") Long userId,
                                        @Param("limit") int limit,
                                        @Param("candidateWindow") int candidateWindow);
```

Keep the existing `findFeedVideos` until snapshot cursor path is complete, so tests can migrate safely.

- [ ] **Step 2: Add seen penalty query**

In `RecommendationMapper.xml`, add a query based on the current feed CTE. Add a left join:

```sql
LEFT JOIN user_video_interactions uvi
  ON uvi.user_id = #{userId}
 AND uvi.video_id = v.id
```

Use penalty ordering:

```sql
CASE
    WHEN uvi.last_viewed_at >= NOW() - INTERVAL '30 days' THEN 2
    WHEN uvi.last_impressed_at >= NOW() - INTERVAL '7 days' THEN 1
    ELSE 0
END AS seen_penalty_rank
```

For embedding-ranked rows, expose penalized distance:

```sql
CASE
    WHEN viewer.style_embedding IS NOT NULL AND g.style_embedding IS NOT NULL
    THEN (viewer.style_embedding <=> g.style_embedding)
         - CASE WHEN f.id IS NOT NULL THEN 0.25 ELSE 0 END
         + CASE
             WHEN uvi.last_viewed_at >= NOW() - INTERVAL '30 days' THEN 1.0
             WHEN uvi.last_impressed_at >= NOW() - INTERVAL '7 days' THEN 0.25
             ELSE 0.0
           END
    ELSE NULL
END AS ranking_distance
```

Order by:

```sql
ORDER BY seen_penalty_rank ASC,
         distance_null_rank ASC,
         ranking_distance ASC,
         following_rank DESC,
         created_at DESC,
         id DESC
LIMIT #{limit}
```

- [ ] **Step 3: Verify integration test**

Run:

```bash
./mvnw -Dtest=RecommendationIntegrationTest#recommendationFeedPenalizesRecentlySeenVideos test
```

Expected: test passes; unseen candidate appears before recently impressed/viewed candidates.

- [ ] **Step 4: Run existing recommendation tests**

Run:

```bash
./mvnw -Dtest=RecommendationIntegrationTest test
```

Expected: all existing recommendation integration tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/holaclimbing/server/domain/recommendation/mapper/RecommendationMapper.java \
        src/main/resources/mapper/recommendation/RecommendationMapper.xml \
        src/test/java/com/holaclimbing/server/domain/recommendation/RecommendationIntegrationTest.java
git commit -m "perf(recommendation): penalize recently seen feed videos"
```

---

### Task 3: Record Actual Views From Video Detail

**Files:**

- Modify: `src/main/java/com/holaclimbing/server/domain/video/service/VideoServiceImpl.java`
- Test: `src/test/java/com/holaclimbing/server/domain/video/VideoIntegrationTest.java`

- [ ] **Step 1: Write failing integration test**

Add a test around `GET /api/videos/{videoId}`:

```java
@Test
@DisplayName("영상 상세 - 인증 사용자가 조회하면 추천 상호작용 조회 기록을 남긴다")
void getVideoDetailRecordsUserVideoViewInteraction() throws Exception {
    // Arrange: create authenticated viewer and public video.
    // Act: GET /api/videos/{id} with viewer token.
    // Assert using JdbcTemplate:
    //   user_video_interactions.viewed_count = 1
    //   last_viewed_at is not null
}
```

Run:

```bash
./mvnw -Dtest=VideoIntegrationTest#getVideoDetailRecordsUserVideoViewInteraction test
```

Expected before implementation: no interaction row is written.

- [ ] **Step 2: Inject mapper and record view**

Modify constructor dependencies by adding:

```java
private final RecommendationInteractionMapper recommendationInteractionMapper;
```

In `getVideoDetail` after access policy passes and after `incrementViewCount(videoId)`:

```java
if (viewerId != null) {
    recommendationInteractionMapper.upsertView(viewerId, videoId);
}
```

Do not record anonymous views; the recommendation feed is authenticated and user-specific.

- [ ] **Step 3: Run focused test**

Run:

```bash
./mvnw -Dtest=VideoIntegrationTest#getVideoDetailRecordsUserVideoViewInteraction test
```

Expected: pass.

- [ ] **Step 4: Run video tests**

Run:

```bash
./mvnw -Dtest=VideoIntegrationTest test
```

Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/holaclimbing/server/domain/video/service/VideoServiceImpl.java \
        src/test/java/com/holaclimbing/server/domain/video/VideoIntegrationTest.java
git commit -m "feat(video): record authenticated video views for recommendations"
```

---

### Task 4: Add Redis Feed Snapshot Store

**Files:**

- Create: `src/main/java/com/holaclimbing/server/domain/recommendation/domain/RecommendationFeedSnapshot.java`
- Create: `src/main/java/com/holaclimbing/server/domain/recommendation/service/RecommendationFeedSnapshotStore.java`
- Test: `src/test/java/com/holaclimbing/server/domain/recommendation/service/RecommendationFeedSnapshotStoreTest.java`

- [ ] **Step 1: Write store unit test**

Create a focused test with mocked `StringRedisTemplate` and `ValueOperations`.

Test cases:

```java
@Test
void saveStoresJsonWithTtlAndReturnsSnapshotId() {
    // Assert key prefix "recommendation:feed:snapshot:{userId}:"
    // Assert TTL Duration.ofMinutes(10)
}

@Test
void findReturnsEmptyWhenRedisValueIsMissing() {
    // Assert Optional.empty()
}
```

- [ ] **Step 2: Create snapshot payload**

Create:

```java
package com.holaclimbing.server.domain.recommendation.domain;

import java.time.Instant;
import java.util.List;

public record RecommendationFeedSnapshot(
        Long userId,
        List<Long> videoIds,
        Instant generatedAt
) {
}
```

- [ ] **Step 3: Create snapshot store**

Use `StringRedisTemplate` and the application `ObjectMapper`:

```java
@Service
@RequiredArgsConstructor
public class RecommendationFeedSnapshotStore {

    private static final String KEY_PREFIX = "recommendation:feed:snapshot:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public String save(Long userId, List<Long> videoIds) {
        String snapshotId = UUID.randomUUID().toString();
        RecommendationFeedSnapshot snapshot = new RecommendationFeedSnapshot(
                userId,
                List.copyOf(videoIds),
                Instant.now());
        redis.opsForValue().set(key(userId, snapshotId), write(snapshot), TTL);
        return snapshotId;
    }

    public Optional<RecommendationFeedSnapshot> find(Long userId, String snapshotId) {
        String json = redis.opsForValue().get(key(userId, snapshotId));
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        RecommendationFeedSnapshot snapshot = read(json);
        if (!userId.equals(snapshot.userId())) {
            return Optional.empty();
        }
        return Optional.of(snapshot);
    }

    private String key(Long userId, String snapshotId) {
        return KEY_PREFIX + userId + ":" + snapshotId;
    }
}
```

Implement `write` and `read` with `JsonProcessingException` wrapped in `IllegalStateException`.

- [ ] **Step 4: Run store test**

Run:

```bash
./mvnw -Dtest=RecommendationFeedSnapshotStoreTest test
```

Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/holaclimbing/server/domain/recommendation/domain/RecommendationFeedSnapshot.java \
        src/main/java/com/holaclimbing/server/domain/recommendation/service/RecommendationFeedSnapshotStore.java \
        src/test/java/com/holaclimbing/server/domain/recommendation/service/RecommendationFeedSnapshotStoreTest.java
git commit -m "feat(recommendation): add Redis feed snapshot store"
```

---

### Task 5: Replace Ranking Cursor With Snapshot Cursor

**Files:**

- Modify: `src/main/java/com/holaclimbing/server/domain/recommendation/dto/RecommendationCursor.java`
- Modify: `src/main/java/com/holaclimbing/server/domain/recommendation/dto/RecommendationCursorCodec.java`
- Test: `src/test/java/com/holaclimbing/server/domain/recommendation/dto/RecommendationCursorCodecTest.java`

- [ ] **Step 1: Write cursor codec tests**

Add tests:

```java
@Test
void snapshotCursorRoundTrip() {
    RecommendationCursor cursor = RecommendationCursor.snapshot("abc", 20);
    String encoded = RecommendationCursorCodec.encode(cursor);
    RecommendationCursor decoded = RecommendationCursorCodec.decode(encoded);
    assertThat(decoded.getSnapshotId()).isEqualTo("abc");
    assertThat(decoded.getOffset()).isEqualTo(20);
}

@Test
void snapshotCursorRejectsNegativeOffset() {
    String invalid = base64UrlJson("{\"type\":\"snapshot\",\"snapshotId\":\"abc\",\"offset\":-1}");
    assertThatThrownBy(() -> RecommendationCursorCodec.decode(invalid))
            .isInstanceOf(BusinessException.class);
}
```

- [ ] **Step 2: Refactor cursor**

Make cursor snapshot-based:

```java
public class RecommendationCursor {

    private final String snapshotId;
    private final int offset;

    private RecommendationCursor(String snapshotId, int offset) {
        this.snapshotId = snapshotId;
        this.offset = offset;
    }

    public static RecommendationCursor snapshot(String snapshotId, int offset) {
        return new RecommendationCursor(snapshotId, offset);
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public int getOffset() {
        return offset;
    }
}
```

- [ ] **Step 3: Refactor codec**

Encode payload:

```json
{
  "type": "snapshot",
  "snapshotId": "uuid",
  "offset": 20
}
```

Validation:

- `type` must equal `snapshot`.
- `snapshotId` must not be blank.
- `offset` must be `>= 0`.

- [ ] **Step 4: Run cursor tests**

Run:

```bash
./mvnw -Dtest=RecommendationCursorCodecTest test
```

Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/holaclimbing/server/domain/recommendation/dto/RecommendationCursor.java \
        src/main/java/com/holaclimbing/server/domain/recommendation/dto/RecommendationCursorCodec.java \
        src/test/java/com/holaclimbing/server/domain/recommendation/dto/RecommendationCursorCodecTest.java
git commit -m "feat(recommendation): use snapshot feed cursors"
```

---

### Task 6: Serve Recommendation Feed From Snapshot

**Files:**

- Modify: `src/main/java/com/holaclimbing/server/domain/recommendation/service/RecommendationServiceImpl.java`
- Modify: `src/main/java/com/holaclimbing/server/domain/recommendation/mapper/RecommendationMapper.java`
- Modify: `src/main/resources/mapper/recommendation/RecommendationMapper.xml`
- Test: `src/test/java/com/holaclimbing/server/domain/recommendation/service/RecommendationServiceImplTest.java`
- Test: `src/test/java/com/holaclimbing/server/domain/recommendation/RecommendationIntegrationTest.java`

- [ ] **Step 1: Add unit test for first page snapshot creation**

Test behavior:

- `cursor == null`
- service calls `findFeedSnapshotCandidates(userId, 1000, candidateWindow)`
- service saves snapshot with candidate IDs
- response returns first `size`
- next cursor is `snapshotId + offset=size`
- impressions are recorded only for returned page IDs

- [ ] **Step 2: Add unit test for cursor page**

Test behavior:

- `cursor != null`
- service loads snapshot
- service does not call `findFeedSnapshotCandidates`
- service fetches the next slice by IDs
- response cursor offset advances by returned content size

- [ ] **Step 3: Add mapper method for ID lookup**

Add:

```java
List<Video> findFeedVideosByIds(@Param("userId") Long userId,
                                @Param("videoIds") List<Long> videoIds);
```

Mapper XML should preserve snapshot order by turning the requested ID list into an ordered CTE:

```xml
WITH requested(video_id, ord) AS (
    SELECT ids.video_id, ids.ord
    FROM unnest(ARRAY[
        <foreach collection="videoIds" item="videoId" separator=",">
            #{videoId}
        </foreach>
    ]::bigint[]) WITH ORDINALITY AS ids(video_id, ord)
),
blocked_users AS (
    SELECT ub.blocked_id AS user_id
    FROM user_blocks ub
    WHERE ub.blocker_id = #{userId}
    UNION
    SELECT ub.blocker_id AS user_id
    FROM user_blocks ub
    WHERE ub.blocked_id = #{userId}
)
SELECT v.id, v.user_id, v.gym_id, v.gym_grade_id,
       g.name AS gym_name,
       gg.label AS gym_grade_label,
       gg.difficulty_order AS gym_grade_difficulty_order,
       v.title, v.description,
       v.gcs_path, v.gcs_streaming_path, v.thumbnail_path,
       v.duration_seconds, v.recorded_date, v.file_size_bytes, v.mime_type,
       v.view_count, v.like_count, v.comment_count, v.status, v.is_public,
       v.created_at, v.updated_at, v.deleted_at
FROM requested r
JOIN videos v ON v.id = r.video_id
JOIN gyms g ON g.id = v.gym_id
JOIN gym_grades gg ON gg.id = v.gym_grade_id AND gg.gym_id = v.gym_id
WHERE v.is_public = TRUE
  AND v.deleted_at IS NULL
  AND v.user_id &lt;&gt; #{userId}
  AND NOT EXISTS (
      SELECT 1
      FROM blocked_users bu
      WHERE bu.user_id = v.user_id
  )
ORDER BY r.ord ASC
```

- [ ] **Step 4: Implement service branch**

Constants:

```java
private static final int DEFAULT_FEED_SNAPSHOT_SIZE = 1_000;
private static final int SNAPSHOT_LOOKAHEAD_MULTIPLIER = 3;
```

First page flow:

```java
List<Video> ranked = recommendationMapper.findFeedSnapshotCandidates(
        userId,
        DEFAULT_FEED_SNAPSHOT_SIZE,
        Math.max(DEFAULT_FEED_CANDIDATE_WINDOW, DEFAULT_FEED_SNAPSHOT_SIZE * 2));
String snapshotId = snapshotStore.save(userId, ranked.stream().map(Video::getId).toList());
List<Video> pageVideos = ranked.stream().limit(size).toList();
recordImpressions(userId, pageVideos);
String nextCursor = pageVideos.size() < ranked.size()
        ? RecommendationCursorCodec.encode(RecommendationCursor.snapshot(snapshotId, pageVideos.size()))
        : null;
```

Cursor flow:

```java
RecommendationCursor decoded = RecommendationCursorCodec.decode(cursor);
RecommendationFeedSnapshot snapshot = snapshotStore.find(userId, decoded.getSnapshotId())
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "만료된 추천 피드 커서입니다. 첫 페이지를 다시 요청하세요."));
List<Long> slice = snapshot.videoIds().subList(decoded.getOffset(), Math.min(snapshot.videoIds().size(), decoded.getOffset() + size * SNAPSHOT_LOOKAHEAD_MULTIPLIER + 1));
List<Video> videos = recommendationMapper.findFeedVideosByIds(userId, slice);
List<Video> pageVideos = videos.stream().limit(size).toList();
recordImpressions(userId, pageVideos);
int nextOffset = decoded.getOffset() + pageVideos.size();
```

Use lookahead so strict filtering can drop blocked/deleted/private videos without instantly returning short pages.

- [ ] **Step 5: Run service tests**

Run:

```bash
./mvnw -Dtest=RecommendationServiceImplTest test
```

Expected: pass.

- [ ] **Step 6: Run integration tests**

Run:

```bash
./mvnw -Dtest=RecommendationIntegrationTest test
```

Expected: pass.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/holaclimbing/server/domain/recommendation/service/RecommendationServiceImpl.java \
        src/main/java/com/holaclimbing/server/domain/recommendation/mapper/RecommendationMapper.java \
        src/main/resources/mapper/recommendation/RecommendationMapper.xml \
        src/test/java/com/holaclimbing/server/domain/recommendation/service/RecommendationServiceImplTest.java \
        src/test/java/com/holaclimbing/server/domain/recommendation/RecommendationIntegrationTest.java
git commit -m "perf(recommendation): serve cursor pages from feed snapshots"
```

---

### Task 7: Update Performance Evidence Scripts

**Files:**

- Modify: `perf/sql/recommendation_feed_explain_text.sql`
- Modify: `perf/sql/recommendation_feed_explain_json.sql`
- Modify: `perf/scripts/report_recommendation_sql.sh`
- Modify: `perf/scripts/render_recommendation_presentation.py`
- Modify: `perf/README.md`

- [ ] **Step 1: Update SQL report variables**

Add:

```bash
SNAPSHOT_SIZE="${SNAPSHOT_SIZE:-1000}"
```

Record it in `code-state.txt`:

```bash
echo "snapshot_size=${SNAPSHOT_SIZE}"
```

- [ ] **Step 2: Update EXPLAIN SQL**

Make the SQL report represent first-page snapshot candidate generation:

```sql
LIMIT :snapshot_size
```

Keep `candidate_window` for the upstream candidate CTE.

- [ ] **Step 3: Add cursor snapshot evidence note**

In `perf/README.md`, document:

```text
After snapshot implementation, SQL EXPLAIN measures first-page snapshot generation.
k6 first_page and cursor_page metrics are the source of cursor snapshot performance evidence.
```

- [ ] **Step 4: Update presentation renderer**

Add labels:

- `snapshot_size`
- `seen penalty`
- `cursor from Redis snapshot`

Ensure the renderer still passes:

```bash
python3 perf/scripts/render_recommendation_presentation.py after
```

Expected:

```text
overflow_check=passed
```

- [ ] **Step 5: Commit**

```bash
git add perf/sql/recommendation_feed_explain_text.sql \
        perf/sql/recommendation_feed_explain_json.sql \
        perf/scripts/report_recommendation_sql.sh \
        perf/scripts/render_recommendation_presentation.py \
        perf/README.md
git commit -m "test(perf): report feed snapshot performance evidence"
```

---

### Task 8: Capture Before/After Evidence

**Files:**

- Modify/create result files under `perf/results/recommendation-feed/after/`
- Update vault after measurement.

- [ ] **Step 1: Run tests**

Run:

```bash
./mvnw -Dtest=RecommendationServiceImplTest,RecommendationIntegrationTest,VideoIntegrationTest test
```

Expected:

```text
BUILD SUCCESS
Tests run: all selected tests pass, Failures: 0, Errors: 0
```

- [ ] **Step 2: Apply migration to local perf DB**

Run against `hola_perf`:

```bash
psql postgresql://hola:hola@127.0.0.1:5432/hola_perf \
  -f src/main/resources/db/migration/V8__recommendation_feed_interactions.sql
```

Expected:

```text
CREATE TABLE
CREATE INDEX
CREATE INDEX
```

- [ ] **Step 3: Run after SQL report**

Run:

```bash
DATABASE_URL=postgresql://hola:hola@127.0.0.1:5432/hola_perf \
RUN_LABEL=after \
VIEWER_ID=1 \
PAGE_SIZE=20 \
CANDIDATE_WINDOW=5000 \
SNAPSHOT_SIZE=1000 \
./perf/scripts/report_recommendation_sql.sh
```

Expected:

```text
SQL report written to perf/results/recommendation-feed/after
```

- [ ] **Step 4: Run after k6**

Run with the same smoke settings used previously:

```bash
BASE_URL=http://localhost:8080 \
RUN_LABEL=after \
TOKEN_USER_COUNT=5 \
VUS=2 \
RAMP_UP=10s \
STEADY=20s \
RAMP_DOWN=5s \
k6 run perf/k6/recommendation-feed.js
```

Expected:

- `http_req_failed` remains `0`.
- `recommendation_feed_cursor_page_duration p95` drops versus previous after `391ms`.
- `recommendation_feed_first_page_duration p95` may stay similar or increase slightly because it creates the snapshot.

- [ ] **Step 5: Render presentation cards**

Run:

```bash
python3 perf/scripts/render_recommendation_presentation.py after
```

Expected:

```text
presentation_cards=5
overflow_check=passed
```

- [ ] **Step 6: Visual review**

Open at least:

- `perf/results/recommendation-feed/after/screenshots/presentation/04-after-before-after-template.png`
- `perf/results/recommendation-feed/after/screenshots/presentation/03-after-k6-result-interpretation.png`
- `perf/results/recommendation-feed/after/screenshots/presentation/05-after-code-change-template.png`

Check:

- Korean result interpretation.
- No text overflow.
- Snapshot and seen penalty labels are visible.
- If first page gets slower but cursor page gets faster, the image says that honestly.

- [ ] **Step 7: Validate evidence**

Run:

```bash
./perf/scripts/validate_recommendation_evidence.sh after
```

Expected:

```text
Recommendation feed evidence is complete for after
```

- [ ] **Step 8: Commit evidence**

```bash
git add perf/results/recommendation-feed/after perf/results/recommendation-feed/local-baseline/screenshots/presentation perf/README.md
git commit -m "test(perf): capture snapshot recommendation feed evidence"
```

- [ ] **Step 9: Vault update**

Append a Korean note to:

```text
/Users/minjoun/Documents/DevKnowledge/50_SessionLogs/2026-06-18-hola-performance-e2e.md
```

Include:

- Scope: snapshot + seen penalty.
- Before/after p95, p99, first page p95, cursor page p95.
- SQL execution time and temp blocks.
- Whether cursor p95 improved.
- Image paths.
- Honest interpretation of trade-offs.

---

## Expected Portfolio Story

Before:

- Recommendation feed ranked candidates every page.
- Cursor pages recomputed the same candidate window.
- Already seen videos could remain near the top because there was no user-specific consumption history.

After:

- First page creates a short-lived Redis snapshot.
- Cursor pages read stable snapshot slices instead of recomputing ranking.
- Viewed/impressed videos receive a ranking penalty.
- Strict filters keep blocked/deleted/private/self videos out even if a stale snapshot contains them.

Expected result:

- Cursor page p95 should drop.
- First page p95 may be similar or slightly higher due to snapshot creation.
- Repeated video exposure should decrease.
- Overall interpretation must separate "first-page generation cost" from "cursor-page scroll cost".

## Self-Review Checklist

- [ ] The plan creates a user-specific interaction table before using seen penalties.
- [ ] The plan distinguishes feed impressions from actual video views.
- [ ] The plan keeps request-time strict filters for privacy and safety.
- [ ] The plan avoids worker/scheduler scope creep.
- [ ] The plan measures both first-page and cursor-page k6 metrics.
- [ ] The plan preserves the user's evidence requirement: raw files plus presentation PNGs.
