# Nearby Gym Recommendation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build `GET /api/recommendations/gyms`, an authenticated nearby gym recommendation API that filters by location and ranks by user-gym style similarity when embeddings exist.

**Architecture:** Keep the feature in the existing recommendation domain. Add a recommendation read model for gym rows, expose a `RecommendedGymResponse`, and implement a MyBatis SQL query that filters active gyms by radius, computes `distanceKm`, computes pgvector cosine distance when possible, and falls back to distance order for sparse embeddings.

**Tech Stack:** Java 25, Spring Boot 4, MyBatis, PostgreSQL pgvector, JUnit 5, MockMvc, Testcontainers.

---

## File Structure

- Create `src/main/java/com/holaclimbing/server/domain/recommendation/domain/RecommendedGym.java`: MyBatis read model for nearby gym recommendations.
- Create `src/main/java/com/holaclimbing/server/domain/recommendation/dto/response/RecommendedGymResponse.java`: API response DTO with `distanceKm`, nullable `rankingDistance`, and `source`.
- Modify `src/main/java/com/holaclimbing/server/domain/recommendation/RecommendationController.java`: add `GET /api/recommendations/gyms`.
- Modify `src/main/java/com/holaclimbing/server/domain/recommendation/service/RecommendationService.java`: add `getNearbyGyms`.
- Modify `src/main/java/com/holaclimbing/server/domain/recommendation/service/RecommendationServiceImpl.java`: validate coordinates and map read models to DTOs.
- Modify `src/main/java/com/holaclimbing/server/domain/recommendation/mapper/RecommendationMapper.java`: add `findNearbyGyms`.
- Modify `src/main/resources/mapper/recommendation/RecommendationMapper.xml`: add SQL query.
- Modify `src/test/java/com/holaclimbing/server/domain/recommendation/RecommendationIntegrationTest.java`: add behavior and validation tests.

---

### Task 1: Nearby Gym Recommendation Tests

**Files:**
- Modify: `src/test/java/com/holaclimbing/server/domain/recommendation/RecommendationIntegrationTest.java`

- [x] **Step 1: Write failing tests**

Add these tests before the helper section:

```java
@Test
@DisplayName("암장 추천 — 내 주변 암장을 스타일 유사도 순으로 반환한다")
void getNearbyGyms_whenEmbeddingsExist_ordersByStyleSimilarity() throws Exception {
    String viewer = register("viewer-gym-rec@hola.com", "viewer");
    setUserEmbedding("viewer-gym-rec@hola.com", 2);

    mockMvc.perform(get("/api/recommendations/gyms")
                    .param("lat", "37.5000")
                    .param("lng", "127.0200")
                    .param("radius", "12")
                    .header("Authorization", "Bearer " + viewer))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value(2))
            .andExpect(jsonPath("$.data[0].name").value("ClimbingPark Hongdae"))
            .andExpect(jsonPath("$.data[0].rankingDistance").isNumber())
            .andExpect(jsonPath("$.data[0].distanceKm").isNumber())
            .andExpect(jsonPath("$.data[0].source").value("style_match"))
            .andExpect(jsonPath("$.data[1].id").value(1))
            .andExpect(jsonPath("$.data[1].source").value("style_match"));
}

@Test
@DisplayName("암장 추천 — 사용자 임베딩이 없으면 거리순 fallback")
void getNearbyGyms_withoutUserEmbedding_ordersByDistance() throws Exception {
    String viewer = register("viewer-gym-nearby@hola.com", "viewer");

    mockMvc.perform(get("/api/recommendations/gyms")
                    .param("lat", "37.5000")
                    .param("lng", "127.0200")
                    .param("radius", "12")
                    .header("Authorization", "Bearer " + viewer))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].rankingDistance").doesNotExist())
            .andExpect(jsonPath("$.data[0].source").value("nearby"))
            .andExpect(jsonPath("$.data[1].id").value(2))
            .andExpect(jsonPath("$.data[1].source").value("nearby"));
}

@Test
@DisplayName("암장 추천 — 반경 밖 암장은 제외된다")
void getNearbyGyms_excludesGymsOutsideRadius() throws Exception {
    String viewer = register("viewer-gym-radius@hola.com", "viewer");

    mockMvc.perform(get("/api/recommendations/gyms")
                    .param("lat", "37.5000")
                    .param("lng", "127.0200")
                    .param("radius", "0.1")
                    .header("Authorization", "Bearer " + viewer))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0));
}

@Test
@DisplayName("암장 추천 실패 — 좌표 범위를 벗어나면 400")
void getNearbyGyms_invalidCoordinates_returns400() throws Exception {
    String viewer = register("viewer-gym-invalid@hola.com", "viewer");

    mockMvc.perform(get("/api/recommendations/gyms")
                    .param("lat", "91")
                    .param("lng", "127.0200")
                    .header("Authorization", "Bearer " + viewer))
            .andExpect(status().isBadRequest());

    mockMvc.perform(get("/api/recommendations/gyms")
                    .param("lat", "37.5000")
                    .param("lng", "181")
                    .header("Authorization", "Bearer " + viewer))
            .andExpect(status().isBadRequest());

    mockMvc.perform(get("/api/recommendations/gyms")
                    .param("lat", "NaN")
                    .param("lng", "127.0200")
                    .header("Authorization", "Bearer " + viewer))
            .andExpect(status().isBadRequest());

    mockMvc.perform(get("/api/recommendations/gyms")
                    .param("lat", "37.5000")
                    .param("lng", "Infinity")
                    .header("Authorization", "Bearer " + viewer))
            .andExpect(status().isBadRequest());

    mockMvc.perform(get("/api/recommendations/gyms")
                    .param("lat", "37.5000")
                    .param("lng", "127.0200")
                    .param("radius", "NaN")
                    .header("Authorization", "Bearer " + viewer))
            .andExpect(status().isBadRequest());

    mockMvc.perform(get("/api/recommendations/gyms")
                    .param("lat", "37.5000")
                    .param("lng", "127.0200")
                    .param("radius", "Infinity")
                    .header("Authorization", "Bearer " + viewer))
            .andExpect(status().isBadRequest());
}

@Test
@DisplayName("암장 추천 실패 — 토큰 없이 호출하면 401")
void getNearbyGyms_withoutToken_returns401() throws Exception {
    mockMvc.perform(get("/api/recommendations/gyms")
                    .param("lat", "37.5000")
                    .param("lng", "127.0200"))
            .andExpect(status().isUnauthorized());
}
```

- [x] **Step 2: Run tests to verify they fail**

Run:

```bash
./mvnw -Dtest=RecommendationIntegrationTest#getNearbyGyms_whenEmbeddingsExist_ordersByStyleSimilarity,RecommendationIntegrationTest#getNearbyGyms_withoutUserEmbedding_ordersByDistance,RecommendationIntegrationTest#getNearbyGyms_excludesGymsOutsideRadius,RecommendationIntegrationTest#getNearbyGyms_invalidCoordinates_returns400,RecommendationIntegrationTest#getNearbyGyms_withoutToken_returns401 test
```

Expected: compilation or 404 failures because `GET /api/recommendations/gyms` does not exist yet.

---

### Task 2: API Model And Controller

**Files:**
- Create: `src/main/java/com/holaclimbing/server/domain/recommendation/domain/RecommendedGym.java`
- Create: `src/main/java/com/holaclimbing/server/domain/recommendation/dto/response/RecommendedGymResponse.java`
- Modify: `src/main/java/com/holaclimbing/server/domain/recommendation/RecommendationController.java`
- Modify: `src/main/java/com/holaclimbing/server/domain/recommendation/service/RecommendationService.java`
- Modify: `src/main/java/com/holaclimbing/server/domain/recommendation/service/RecommendationServiceImpl.java`

- [x] **Step 1: Add read model**

Create `RecommendedGym.java`:

```java
package com.holaclimbing.server.domain.recommendation.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedGym {

    private Long id;
    private String name;
    private String address;
    private String thumbnailUrl;
    private String regionCode;
    private BigDecimal ratingAvg;
    private int ratingCount;
    private Double distanceKm;
    private Double rankingDistance;
}
```

- [x] **Step 2: Add response DTO**

Create `RecommendedGymResponse.java`:

```java
package com.holaclimbing.server.domain.recommendation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.holaclimbing.server.domain.recommendation.domain.RecommendedGym;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecommendedGymResponse(
        Long id,
        String name,
        String address,
        String thumbnailUrl,
        String regionCode,
        BigDecimal ratingAvg,
        int ratingCount,
        Double distanceKm,
        Double rankingDistance,
        String source
) {
    private static final String SOURCE_STYLE_MATCH = "style_match";
    private static final String SOURCE_NEARBY = "nearby";

    public static RecommendedGymResponse from(RecommendedGym gym) {
        return new RecommendedGymResponse(
                gym.getId(),
                gym.getName(),
                gym.getAddress(),
                gym.getThumbnailUrl(),
                gym.getRegionCode(),
                gym.getRatingAvg(),
                gym.getRatingCount(),
                gym.getDistanceKm(),
                gym.getRankingDistance(),
                gym.getRankingDistance() == null ? SOURCE_NEARBY : SOURCE_STYLE_MATCH);
    }
}
```

- [x] **Step 3: Add service signature**

Modify `RecommendationService.java`:

```java
List<RecommendedGymResponse> getNearbyGyms(Long userId, double lat, double lng, double radiusKm, int size);
```

Also add imports:

```java
import com.holaclimbing.server.domain.recommendation.dto.response.RecommendedGymResponse;
import java.util.List;
```

- [x] **Step 4: Add controller endpoint**

In `RecommendationController.java`, add imports:

```java
import com.holaclimbing.server.domain.recommendation.dto.response.RecommendedGymResponse;
import java.util.List;
```

Add this method:

```java
@GetMapping("/gyms")
public ApiResponse<List<RecommendedGymResponse>> getNearbyGyms(
        @AuthenticationPrincipal Long userId,
        @RequestParam double lat,
        @RequestParam double lng,
        @RequestParam(defaultValue = "10") @Positive double radius,
        @RequestParam(defaultValue = "20") @Positive @Max(100) int size) {
    return ApiResponse.success(recommendationService.getNearbyGyms(userId, lat, lng, radius, size));
}
```

- [x] **Step 5: Add service implementation**

In `RecommendationServiceImpl.java`, add:

```java
private static final double MIN_LAT = -90.0;
private static final double MAX_LAT = 90.0;
private static final double MIN_LNG = -180.0;
private static final double MAX_LNG = 180.0;
```

Add imports:

```java
import com.holaclimbing.server.common.exception.BusinessException;
import com.holaclimbing.server.common.exception.error.ErrorCode;
import com.holaclimbing.server.domain.recommendation.dto.response.RecommendedGymResponse;
```

Add method:

```java
@Override
public List<RecommendedGymResponse> getNearbyGyms(Long userId, double lat, double lng, double radiusKm, int size) {
    validateCoordinates(lat, lng);
    return recommendationMapper.findNearbyGyms(userId, lat, lng, radiusKm, size)
            .stream()
            .map(RecommendedGymResponse::from)
            .toList();
}

private void validateCoordinates(double lat, double lng) {
    if (lat < MIN_LAT || lat > MAX_LAT) {
        throw new BusinessException(ErrorCode.INVALID_INPUT, "위도는 -90~90 범위여야 합니다.");
    }
    if (lng < MIN_LNG || lng > MAX_LNG) {
        throw new BusinessException(ErrorCode.INVALID_INPUT, "경도는 -180~180 범위여야 합니다.");
    }
}
```

---

### Task 3: Mapper SQL

**Files:**
- Modify: `src/main/java/com/holaclimbing/server/domain/recommendation/mapper/RecommendationMapper.java`
- Modify: `src/main/resources/mapper/recommendation/RecommendationMapper.xml`

- [x] **Step 1: Add mapper signature**

Add import:

```java
import com.holaclimbing.server.domain.recommendation.domain.RecommendedGym;
```

Add method:

```java
/** 주변 암장 추천 — 반경 필터 + 스타일 유사도 우선 + 거리 fallback. */
List<RecommendedGym> findNearbyGyms(@Param("userId") Long userId,
                                    @Param("lat") double lat,
                                    @Param("lng") double lng,
                                    @Param("radiusKm") double radiusKm,
                                    @Param("limit") int limit);
```

- [x] **Step 2: Add SQL**

Add this select after `findFeedVideos`:

```xml
<select id="findNearbyGyms" resultType="com.holaclimbing.server.domain.recommendation.domain.RecommendedGym">
    WITH viewer AS (
        SELECT style_embedding
        FROM users
        WHERE id = #{userId}
    ),
    candidate AS (
        SELECT g.id, g.name, g.address, g.thumbnail_url, g.region_code,
               g.rating_avg, g.rating_count,
               6371 * acos(GREATEST(-1.0, LEAST(1.0,
                   cos(radians(#{lat})) * cos(radians(g.lat))
                   * cos(radians(g.lng) - radians(#{lng}))
                   + sin(radians(#{lat})) * sin(radians(g.lat))
               ))) AS distance_km,
               CASE
                   WHEN viewer.style_embedding IS NOT NULL AND g.style_embedding IS NOT NULL
                   THEN viewer.style_embedding &lt;=&gt; g.style_embedding
                   ELSE NULL
               END AS ranking_distance
        FROM gyms g
        CROSS JOIN viewer
        WHERE g.lat IS NOT NULL
          AND g.lng IS NOT NULL
          AND g.status = 'active'
          AND g.deleted_at IS NULL
    )
    SELECT id, name, address, thumbnail_url, region_code,
           rating_avg, rating_count, distance_km, ranking_distance
    FROM candidate
    WHERE distance_km &lt;= #{radiusKm}
    ORDER BY
        CASE WHEN ranking_distance IS NULL THEN 1 ELSE 0 END ASC,
        ranking_distance ASC,
        distance_km ASC,
        rating_avg DESC,
        rating_count DESC,
        id ASC
    LIMIT #{limit}
</select>
```

- [x] **Step 3: Run nearby gym tests**

Run:

```bash
./mvnw -Dtest=RecommendationIntegrationTest#getNearbyGyms_whenEmbeddingsExist_ordersByStyleSimilarity,RecommendationIntegrationTest#getNearbyGyms_withoutUserEmbedding_ordersByDistance,RecommendationIntegrationTest#getNearbyGyms_excludesGymsOutsideRadius,RecommendationIntegrationTest#getNearbyGyms_invalidCoordinates_returns400,RecommendationIntegrationTest#getNearbyGyms_withoutToken_returns401 test
```

Expected: PASS.

---

### Task 4: Regression And Documentation Hooks

**Files:**
- Modify: `docs/superpowers/plans/2026-06-10-nearby-gym-recommendation.md`
- Later Notion update: `API 명세서 (내부)`

- [x] **Step 1: Run recommendation regression tests**

Run:

```bash
./mvnw -Dtest=RecommendationIntegrationTest test
```

Expected: PASS.

- [x] **Step 2: Run adjacent gym and full tests if time permits**

Run:

```bash
./mvnw -Dtest=GymIntegrationTest,RecommendationIntegrationTest test
```

Expected: PASS.

Then run:

```bash
./mvnw test
```

Expected: PASS.

- [x] **Step 3: Document in Notion and vault**

Create or update Notion API docs:

- 기능: `암장 추천`
- HTTP 메서드: `GET`
- API Path: `/api/recommendations/gyms`
- 분류: `추천`, `암장`
- 백: `Done`
- 프론트: `Not started`

Append a session log under `50_SessionLogs/2026-06-10-hola-climbing-server-nearby-gym-recommendation.md` after implementation and verification.

---

## Self-Review

- Spec coverage: endpoint, auth, query defaults, ranking, response fields, error handling, tests, Notion follow-up, and rollback cost are covered.
- Placeholder scan: no TBD/TODO/fill-in placeholders remain.
- Type consistency: `radiusKm` is used in service/mapper, query param remains `radius`, `rankingDistance` and `distanceKm` are Java `Double`, and response `source` values are `style_match` or `nearby`.
