# Recommendation Feed Query Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 추천 피드의 1차 성능 개선 실험으로 `user_blocks` 조회를 안전하게 개선하고, 추천 후보군 window 제한을 적용해 SQL 실행시간, scan rows, temp blocks를 줄인다.

**Architecture:** 기능 요구사항은 유지한다: 팔로잉 boost, 벡터 유사도 정렬, 본인 영상 제외, 상호 차단 제외, cursor pagination. 개선은 `RecommendationMapper.xml`의 SQL과 보조 인덱스에 한정하고, 동일 k6/SQL report/presentation evidence 흐름으로 before/after를 비교한다.

**Tech Stack:** Spring Boot 4, MyBatis XML mapper, PostgreSQL + pgvector, Flyway, k6, psql, Pillow presentation renderer.

---

## Scope Check

이번 계획은 아래 두 개선만 포함한다.

- 1번: `user_blocks` 조회 안전 개선
- 2번: 추천 후보군 window 제한 실험

이번 계획은 feed cache, 사전 랭킹 테이블, worker/scheduler, Cloud Run autoscaling 튜닝을 포함하지 않는다. after 결과가 충분하지 않을 때 별도 계획으로 확장한다.

## Current Baseline

현재 local-baseline 측정값:

```text
p95=251ms
p99=260ms
error_rate=0%
SQL execution=181ms
temp read blocks=1405
temp written blocks=9138
scan rows ~= 99,990 videos
```

현재 병목 해석:

- `RecommendationServiceImpl`은 `size + 1`만 조회하므로 API 서비스 계층의 반복 처리 병목은 작다.
- `RecommendationMapper.xml`의 `findFeedVideos`가 공개 영상 후보 전체를 대상으로 `ranking_distance`, `following_rank`, `created_at`, `id` 정렬을 수행한다.
- 계산 컬럼 기반 정렬 때문에 `idx_videos_public_feed`만으로 최종 정렬을 피하기 어렵고, 현재 plan은 `external merge sort`와 temp block write를 만든다.
- `user_blocks`는 현재 작지만 OR 조건이 있고 `blocked_id` 방향 단독 인덱스가 없어 그래프가 커질 때 위험하다.

## File Structure

Create these files:

- `src/main/resources/db/migration/V5__recommendation_feed_query_indexes.sql`
  - Add supporting indexes for the two planned SQL changes.

Modify these files:

- `src/main/resources/mapper/recommendation/RecommendationMapper.xml`
  - Split block filtering into index-friendly blocked-user lookup.
  - Add a recent public video candidate window before expensive ranking.
- `src/main/java/com/holaclimbing/server/domain/recommendation/mapper/RecommendationMapper.java`
  - Add `candidateWindow` parameter to `findFeedVideos`.
- `src/main/java/com/holaclimbing/server/domain/recommendation/service/RecommendationServiceImpl.java`
  - Compute and pass a bounded candidate window size.
- `perf/scripts/report_recommendation_sql.sh`
  - Add `CANDIDATE_WINDOW` so SQL reports match production mapper behavior.
- `perf/sql/recommendation_feed_explain_text.sql`
  - Mirror the optimized mapper query for screenshot-friendly EXPLAIN.
- `perf/sql/recommendation_feed_explain_json.sql`
  - Mirror the optimized mapper query for machine-readable EXPLAIN.
- `perf/README.md`
  - Add after-run instructions for this optimization experiment.
- `perf/scripts/render_recommendation_presentation.py`
  - Generalize hard-coded `local-baseline` labels and output filenames so `after` evidence is not mislabeled.

Test these files:

- `src/test/java/com/holaclimbing/server/domain/recommendation/RecommendationIntegrationTest.java`
  - Existing tests cover following-first, block exclusion, reverse block exclusion, vector ordering, following boost, cursor pagination, invalid cursor, and auth.
  - Add one focused regression test only if candidate window behavior needs an explicit small-window assertion.

Evidence outputs:

- `perf/results/recommendation-feed/after/k6-summary.json`
- `perf/results/recommendation-feed/after/k6-summary.txt`
- `perf/results/recommendation-feed/after/recommendation-feed-explain.json`
- `perf/results/recommendation-feed/after/recommendation-feed-explain.txt`
- `perf/results/recommendation-feed/after/row-counts-and-sizes.txt`
- `perf/results/recommendation-feed/after/code-state.txt`
- `perf/results/recommendation-feed/after/screenshots/*`
- `perf/results/recommendation-feed/after/screenshots/presentation/*`

## Task 1: Add Supporting Index Migration

**Files:**
- Create: `src/main/resources/db/migration/V5__recommendation_feed_query_indexes.sql`

- [ ] **Step 1: Create the migration**

Create `src/main/resources/db/migration/V5__recommendation_feed_query_indexes.sql`:

```sql
CREATE INDEX IF NOT EXISTS idx_user_blocks_blocked
    ON user_blocks(blocked_id);

CREATE INDEX IF NOT EXISTS idx_videos_public_feed_recent
    ON videos(created_at DESC, id DESC)
    WHERE is_public = TRUE AND deleted_at IS NULL;
```

- [ ] **Step 2: Run migration-aware tests**

Run:

```bash
./mvnw -Dtest=RecommendationIntegrationTest test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 3: Commit the migration**

Run:

```bash
git add src/main/resources/db/migration/V5__recommendation_feed_query_indexes.sql
git commit -m "perf(recommendation): add feed query support indexes"
```

Expected: commit succeeds and only the migration file is included.

## Task 2: Make `user_blocks` Filtering Index-Friendly

**Files:**
- Modify: `src/main/resources/mapper/recommendation/RecommendationMapper.xml`
- Modify: `perf/sql/recommendation_feed_explain_text.sql`
- Modify: `perf/sql/recommendation_feed_explain_json.sql`

- [ ] **Step 1: Change mapper SQL block filtering**

In `src/main/resources/mapper/recommendation/RecommendationMapper.xml`, replace the current `NOT EXISTS` OR block:

```xml
AND NOT EXISTS (
    SELECT 1
    FROM user_blocks ub
    WHERE (ub.blocker_id = #{userId} AND ub.blocked_id = v.user_id)
       OR (ub.blocker_id = v.user_id AND ub.blocked_id = #{userId})
)
```

with an index-friendly blocked-user CTE and lookup:

```xml
blocked_users AS (
    SELECT ub.blocked_id AS user_id
    FROM user_blocks ub
    WHERE ub.blocker_id = #{userId}
    UNION
    SELECT ub.blocker_id AS user_id
    FROM user_blocks ub
    WHERE ub.blocked_id = #{userId}
),
```

Then use it inside the feed filter:

```xml
AND NOT EXISTS (
    SELECT 1
    FROM blocked_users bu
    WHERE bu.user_id = v.user_id
)
```

The CTE order should become:

```xml
WITH viewer AS (...),
blocked_users AS (...),
feed AS (...)
```

- [ ] **Step 2: Apply the same SQL shape to text EXPLAIN**

In `perf/sql/recommendation_feed_explain_text.sql`, add:

```sql
blocked_users AS (
    SELECT ub.blocked_id AS user_id
    FROM user_blocks ub
    WHERE ub.blocker_id = :viewer_id
    UNION
    SELECT ub.blocker_id AS user_id
    FROM user_blocks ub
    WHERE ub.blocked_id = :viewer_id
),
```

and replace the OR-based `user_blocks` `NOT EXISTS` with:

```sql
AND NOT EXISTS (
    SELECT 1
    FROM blocked_users bu
    WHERE bu.user_id = v.user_id
)
```

- [ ] **Step 3: Apply the same SQL shape to JSON EXPLAIN**

In `perf/sql/recommendation_feed_explain_json.sql`, make the same `blocked_users` CTE and `NOT EXISTS` replacement as Step 2.

- [ ] **Step 4: Run recommendation integration tests**

Run:

```bash
./mvnw -Dtest=RecommendationIntegrationTest test
```

Expected:

```text
BUILD SUCCESS
```

The existing tests that must continue to pass:

- `getVideoFeed_excludesBlockedUploaderVideos`
- `getVideoFeed_excludesUploaderWhoBlockedViewer`
- `getVideoFeed_followingFirst`
- `getVideoFeed_whenEmbeddingsExist_ordersByVectorSimilarity`
- `getVideoFeed_whenFollowingVideoIsClose_appliesFollowingBoost`
- `getVideoFeed_cursorPagination`

- [ ] **Step 5: Capture SQL-only after signal for block lookup**

If a local `hola_perf` database is running, run:

```bash
RUN_LABEL=after-block-filter \
DATABASE_URL=postgresql://hola:hola@127.0.0.1:5432/hola_perf \
./perf/scripts/report_recommendation_sql.sh
```

Expected:

```text
SQL report written to .../perf/results/recommendation-feed/after-block-filter
```

Use this run only as an intermediate signal. The final portfolio after-run should be captured after Task 3.

- [ ] **Step 6: Commit block-filter change**

Run:

```bash
git add src/main/resources/mapper/recommendation/RecommendationMapper.xml \
  perf/sql/recommendation_feed_explain_text.sql \
  perf/sql/recommendation_feed_explain_json.sql
git commit -m "perf(recommendation): optimize block filtering in feed query"
```

Expected: commit succeeds and does not include generated performance outputs unless intentionally captured.

## Task 3: Add Candidate Window Parameter

**Files:**
- Modify: `src/main/java/com/holaclimbing/server/domain/recommendation/mapper/RecommendationMapper.java`
- Modify: `src/main/java/com/holaclimbing/server/domain/recommendation/service/RecommendationServiceImpl.java`
- Modify: `src/main/resources/mapper/recommendation/RecommendationMapper.xml`

- [ ] **Step 1: Add mapper parameter**

Change `RecommendationMapper.findFeedVideos` from:

```java
List<Video> findFeedVideos(@Param("userId") Long userId,
                           @Param("cursor") RecommendationCursor cursor,
                           @Param("limit") int limit);
```

to:

```java
List<Video> findFeedVideos(@Param("userId") Long userId,
                           @Param("cursor") RecommendationCursor cursor,
                           @Param("limit") int limit,
                           @Param("candidateWindow") int candidateWindow);
```

- [ ] **Step 2: Add service-level window constants**

In `RecommendationServiceImpl`, add:

```java
private static final int DEFAULT_FEED_CANDIDATE_WINDOW = 5_000;
private static final int MIN_FEED_CANDIDATE_WINDOW_MULTIPLIER = 50;
```

Then change the mapper call from:

```java
List<Video> videos = recommendationMapper.findFeedVideos(userId, decodedCursor, size + 1);
```

to:

```java
int limit = size + 1;
int candidateWindow = Math.max(DEFAULT_FEED_CANDIDATE_WINDOW, limit * MIN_FEED_CANDIDATE_WINDOW_MULTIPLIER);
List<Video> videos = recommendationMapper.findFeedVideos(userId, decodedCursor, limit, candidateWindow);
```

Reasoning:

- `5,000` cuts the current 100k candidate set by about 95%.
- `limit * 50` keeps the window proportional if a larger page size is requested.
- This is intentionally a first experiment, not the final recommendation architecture.

- [ ] **Step 3: Add `candidate_videos` CTE**

In `RecommendationMapper.xml`, insert `candidate_videos` after `blocked_users`:

```xml
candidate_videos AS (
    SELECT v.*
    FROM videos v
    WHERE v.is_public = TRUE
      AND v.deleted_at IS NULL
      AND v.user_id &lt;&gt; #{userId}
      AND NOT EXISTS (
          SELECT 1
          FROM blocked_users bu
          WHERE bu.user_id = v.user_id
      )
    ORDER BY v.created_at DESC, v.id DESC
    LIMIT #{candidateWindow}
),
```

Then change the feed source from:

```xml
FROM videos v
```

to:

```xml
FROM candidate_videos v
```

And remove duplicated base filters from the `feed` CTE:

```xml
WHERE v.is_public = TRUE AND v.deleted_at IS NULL
  AND v.user_id &lt;&gt; #{userId}
  AND NOT EXISTS (...)
```

The expensive joins and ranking calculation should now run against `candidate_videos`, not all public `videos`.

- [ ] **Step 4: Run recommendation integration tests**

Run:

```bash
./mvnw -Dtest=RecommendationIntegrationTest test
```

Expected:

```text
BUILD SUCCESS
```

If `getVideoFeed_cursorPagination` fails, inspect whether the candidate window was applied before enough rows are available in the small test dataset. With the default `5,000`, the existing integration dataset should not be truncated.

- [ ] **Step 5: Commit candidate window implementation**

Run:

```bash
git add src/main/java/com/holaclimbing/server/domain/recommendation/mapper/RecommendationMapper.java \
  src/main/java/com/holaclimbing/server/domain/recommendation/service/RecommendationServiceImpl.java \
  src/main/resources/mapper/recommendation/RecommendationMapper.xml
git commit -m "perf(recommendation): limit feed ranking candidate window"
```

Expected: commit succeeds with only implementation files.

## Task 4: Align SQL Report Scripts With Candidate Window

**Files:**
- Modify: `perf/scripts/report_recommendation_sql.sh`
- Modify: `perf/sql/recommendation_feed_explain_text.sql`
- Modify: `perf/sql/recommendation_feed_explain_json.sql`
- Modify: `perf/README.md`

- [ ] **Step 1: Add report variable**

In `perf/scripts/report_recommendation_sql.sh`, add after `PAGE_SIZE`:

```bash
CANDIDATE_WINDOW="${CANDIDATE_WINDOW:-5000}"
```

Add this to `code-state.txt` output:

```bash
echo "candidate_window=${CANDIDATE_WINDOW}"
```

Pass it to both EXPLAIN calls:

```bash
-v candidate_window="${CANDIDATE_WINDOW}" \
```

The two affected `psql` commands are the text EXPLAIN and JSON EXPLAIN commands.

- [ ] **Step 2: Add candidate window to text EXPLAIN SQL**

In `perf/sql/recommendation_feed_explain_text.sql`, add the same `candidate_videos` CTE as Task 3, using psql syntax:

```sql
candidate_videos AS (
    SELECT v.*
    FROM videos v
    WHERE v.is_public = TRUE
      AND v.deleted_at IS NULL
      AND v.user_id <> :viewer_id
      AND NOT EXISTS (
          SELECT 1
          FROM blocked_users bu
          WHERE bu.user_id = v.user_id
      )
    ORDER BY v.created_at DESC, v.id DESC
    LIMIT :candidate_window
),
```

Then use:

```sql
FROM candidate_videos v
```

inside the `feed` CTE.

- [ ] **Step 3: Add candidate window to JSON EXPLAIN SQL**

In `perf/sql/recommendation_feed_explain_json.sql`, make the same `candidate_videos` CTE change as Step 2.

- [ ] **Step 4: Update README after-run command**

In `perf/README.md`, add this command near the local SQL report instructions:

```bash
RUN_LABEL=after \
CANDIDATE_WINDOW=5000 \
DATABASE_URL=postgresql://hola:hola@127.0.0.1:5432/hola_perf \
./perf/scripts/report_recommendation_sql.sh
```

Mention that `CANDIDATE_WINDOW` must match the value used by the application.

- [ ] **Step 5: Run report script syntax check**

Run:

```bash
bash -n perf/scripts/report_recommendation_sql.sh
```

Expected: no output.

- [ ] **Step 6: Commit report alignment**

Run:

```bash
git add perf/scripts/report_recommendation_sql.sh \
  perf/sql/recommendation_feed_explain_text.sql \
  perf/sql/recommendation_feed_explain_json.sql \
  perf/README.md
git commit -m "test(perf): align recommendation SQL report with candidate window"
```

Expected: commit succeeds with only perf tooling/docs files.

## Task 5: Generalize Presentation Renderer For After Runs

**Files:**
- Modify: `perf/scripts/render_recommendation_presentation.py`
- Modify: `perf/README.md`

- [ ] **Step 1: Add run label display helper**

Add this helper near `presentation_output`:

```python
def run_display_name(run_label):
    if run_label == "local-baseline":
        return "local-baseline"
    if run_label == "after":
        return "after"
    return run_label
```

- [ ] **Step 2: Replace hard-coded local-baseline titles**

In each renderer function, replace visible text like:

```python
"local-baseline 결과 요약 - GET /api/recommendations/videos?size=20"
```

with:

```python
f"{run_display_name(run_label)} 결과 요약 - GET /api/recommendations/videos?size=20"
```

Apply the same pattern to:

```text
local-baseline EXPLAIN ANALYZE
local-baseline 부하 결과
Before: local-baseline
```

For the before/after comparison card, keep the left side as `Before: local-baseline` only when rendering the baseline. When rendering `after`, use:

```text
Before: local-baseline
After: after
```

If both `local-baseline` and `after` result files exist, fill the comparison rows from both run directories. If `after` files do not exist yet, keep `측정 대기` values. This keeps baseline rendering usable before the after run while preventing after images from being mislabeled.

- [ ] **Step 3: Make output filenames run-label aware**

Change `PRESENTATION_CARDS` filenames from hard-coded `local-baseline` names to format templates:

```python
PRESENTATION_CARDS = {
    "summary": ("01-{run_label}-summary-card.png", render),
    "sql-bottleneck": ("02-{run_label}-sql-bottleneck.png", render_sql_bottleneck),
    "k6-results": ("03-{run_label}-k6-result-interpretation.png", render_k6_results),
    "before-after": ("04-{run_label}-before-after-template.png", render_before_after_template),
    "code-change": ("05-{run_label}-code-change-template.png", render_code_change_template),
}
```

Then resolve filenames with:

```python
filename = filename_template.format(run_label=args.run_label)
```

- [ ] **Step 4: Run renderer for baseline**

Run:

```bash
/Users/minjoun/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 \
  perf/scripts/render_recommendation_presentation.py local-baseline
```

Expected:

```text
presentation_cards=5
overflow_check=passed
```

Open the baseline images and verify they still show `local-baseline` correctly.

- [ ] **Step 5: Update README renderer note**

In `perf/README.md`, add:

```text
The renderer uses the run label in visible titles and filenames. Do not use an
after image that still says local-baseline.
```

- [ ] **Step 6: Commit renderer generalization**

Run:

```bash
git add perf/scripts/render_recommendation_presentation.py perf/README.md \
  perf/results/recommendation-feed/local-baseline/screenshots/presentation
git commit -m "docs(perf): support after presentation evidence labels"
```

Expected: commit succeeds and baseline presentation images remain readable.

## Task 6: Capture Final After Evidence

**Files:**
- Generate: `perf/results/recommendation-feed/after/*`
- Generate: `perf/results/recommendation-feed/after/screenshots/*`
- Generate: `perf/results/recommendation-feed/after/screenshots/presentation/*`

- [ ] **Step 1: Ensure local app uses the optimized code**

Start the local backend against `hola_perf` with Flyway enabled so `V5__recommendation_feed_query_indexes.sql` is applied.

Expected app behavior:

```text
GET /api/recommendations/videos?size=20
```

still returns `200`, `isSuccess=true`, `data.content` as an array, and a valid `nextCursor` when more rows exist.

- [ ] **Step 2: Capture SQL report**

Run:

```bash
RUN_LABEL=after \
CANDIDATE_WINDOW=5000 \
DATABASE_URL=postgresql://hola:hola@127.0.0.1:5432/hola_perf \
./perf/scripts/report_recommendation_sql.sh
```

Expected:

```text
SQL report written to .../perf/results/recommendation-feed/after
```

- [ ] **Step 3: Run k6 after test**

Run the same k6 scenario as baseline, changing only `RUN_LABEL`:

```bash
BASE_URL=http://127.0.0.1:8080 \
RUN_LABEL=after \
PAGE_SIZE=20 \
VUS=5 \
DURATION=30s \
RAMP_UP=5s \
RAMP_DOWN=5s \
k6 run perf/k6/recommendation-feed.js
```

Expected:

```text
checks.........................: 100.00%
http_req_failed................: 0.00%
```

The exact latency is not predetermined. The target is:

```text
SQL execution time lower than 181ms
temp written blocks lower than 9138
scan rows lower than ~99,990
p95 lower than 251ms or at least not worse
```

- [ ] **Step 4: Save screenshots**

Save these images:

```text
perf/results/recommendation-feed/after/screenshots/01-after-recommendation-feed-code-state.png
perf/results/recommendation-feed/after/screenshots/02-after-recommendation-feed-k6-summary.png
perf/results/recommendation-feed/after/screenshots/03-after-recommendation-feed-sql-plan.png
perf/results/recommendation-feed/after/screenshots/04-after-recommendation-feed-query-diff.png
```

Each screenshot must be readable by a human reviewer. Text must not be cropped or visually hidden.

- [ ] **Step 5: Render presentation evidence**

Run:

```bash
/Users/minjoun/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 \
  perf/scripts/render_recommendation_presentation.py after
```

Expected:

```text
presentation_cards=5
overflow_check=passed
```

- [ ] **Step 6: Open generated images for visual QA**

Open each generated after presentation image and check:

```text
result explanations are Korean
technical names can remain English
text stays inside cards
no text overlaps borders or labels
raw evidence paths remain visible
```

- [ ] **Step 7: Validate evidence package**

Run:

```bash
./perf/scripts/validate_recommendation_evidence.sh after
```

Expected:

```text
Recommendation feed evidence is complete for after
```

- [ ] **Step 8: Commit after evidence**

Run:

```bash
git add perf/results/recommendation-feed/after
git commit -m "test(perf): capture recommendation feed after evidence"
```

Expected: commit succeeds with raw after evidence and screenshots.

## Task 7: Compare Before/After and Decide Next Step

**Files:**
- Modify: `perf/results/recommendation-feed/after/screenshots/presentation/04-local-baseline-before-after-template.png` only if the renderer is extended to fill after values.
- Modify: `docs/performance/recommendation-feed-report.md` if the report is still active.
- Append: vault session log under `/Users/minjoun/Documents/DevKnowledge/50_SessionLogs/2026-06-18-hola-performance-e2e.md`.

- [ ] **Step 1: Compare raw metrics**

Compare:

```text
before p95=251ms
before p99=260ms
before SQL execution=181ms
before temp written=9138
before scan rows ~= 99,990
```

against after values from:

```text
perf/results/recommendation-feed/after/k6-summary.json
perf/results/recommendation-feed/after/recommendation-feed-explain.json
```

- [ ] **Step 2: Write Korean result interpretation**

Use this structure:

```markdown
### 추천 피드 1차 개선 결과

- 적용: user_blocks 조회 분리, 최근 공개 영상 candidate window 5000 적용.
- 개선된 지표: ...
- 유지된 지표: ...
- 악화되었거나 확인이 필요한 지표: ...
- 해석: ...
- 다음 판단: 1차 개선으로 충분 / window 크기 조정 필요 / cache 설계 필요.
```

- [ ] **Step 3: Record vault decision**

Append to:

```text
/Users/minjoun/Documents/DevKnowledge/50_SessionLogs/2026-06-18-hola-performance-e2e.md
```

Required content:

```text
- 어떤 개선안을 선택했는지
- 왜 3번 cache/scheduler는 제외했는지
- before/after 핵심 수치
- 생성한 사진 경로
- 결과 해석
- 다음 작업 판단
```

- [ ] **Step 4: Commit comparison docs if repository files changed**

Run:

```bash
git status --short
```

If repository docs or generated comparison images changed, commit them:

```bash
git add <changed repository files>
git commit -m "docs(perf): summarize recommendation feed before after results"
```

Expected: working tree is clean after commit.

## Success Criteria

The plan is successful when all of these are true:

- `RecommendationIntegrationTest` passes after the SQL changes.
- `GET /api/recommendations/videos?size=20` still returns the same response shape.
- SQL report for `after` shows fewer ranked candidates than baseline.
- SQL report for `after` shows reduced `temp written blocks` or removes disk sort.
- k6 `after` run has `error_rate=0%`.
- Presentation screenshots are Korean, readable, and pass `overflow_check=passed`.
- Vault contains the decision, tradeoff, result, and evidence paths.

## Risk Notes

- Candidate window is intentionally lossy. It may skip older but highly similar videos. For this first experiment, that tradeoff is acceptable only if product policy accepts "recent candidate pool first, then personalized ranking."
- If p95 improves but recommendation quality becomes questionable, increase `candidateWindow` and rerun after evidence.
- If SQL improves but k6 p95 does not, inspect URL signing, network, JVM, and DB connection pool metrics before adding more query changes.
- If `temp written blocks` remains high, the next query-level experiment should split following and recommended candidates before merging, but that is outside this plan.
