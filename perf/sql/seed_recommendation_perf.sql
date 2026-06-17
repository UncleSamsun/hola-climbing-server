\set ON_ERROR_STOP on

DO $$
BEGIN
    IF current_database() <> 'hola_perf' THEN
        RAISE EXCEPTION 'Refusing to seed database %. Use hola_perf only.', current_database();
    END IF;
END
$$;

CREATE OR REPLACE FUNCTION perf_vec(seed bigint)
RETURNS vector(64)
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT ('[' || string_agg(
        (((seed * 1103515245 + i * 12345) % 1000)::numeric / 1000)::text,
        ',' ORDER BY i
    ) || ']')::vector(64)
    FROM generate_series(1, 64) AS i;
$$;

TRUNCATE TABLE users, gyms RESTART IDENTITY CASCADE;

INSERT INTO users (
    id,
    email,
    password_hash,
    email_verified,
    nickname,
    role,
    status,
    style_embedding,
    created_at,
    updated_at
)
SELECT
    id,
    format('perf_user_%s@hola.test', lpad(id::text, 5, '0')),
    '$2a$10$bllcflF6BnpYq2vkWimEUO.jLCQsTiBu.OhfuOLRQ8CE4Ko8pYlfu',
    true,
    format('perf_user_%s', lpad(id::text, 5, '0')),
    'USER',
    'ACTIVE',
    CASE WHEN id % 5 = 0 THEN NULL ELSE perf_vec(id) END,
    now() - ((10000 - id) || ' seconds')::interval,
    now()
FROM generate_series(1, 10000) AS id;

INSERT INTO gyms (
    id,
    name,
    address,
    lat,
    lng,
    description,
    region_code,
    rating_avg,
    rating_count,
    status,
    style_embedding,
    created_at,
    updated_at
)
SELECT
    id,
    format('Perf Gym %s', lpad(id::text, 4, '0')),
    format('Seoul Performance-gu %s-gil', id),
    37.45 + (id % 100)::double precision / 1000,
    127.00 + (id % 100)::double precision / 1000,
    'performance seed gym',
    CASE WHEN id % 3 = 0 THEN 'seoul' WHEN id % 3 = 1 THEN 'gyeonggi' ELSE 'incheon' END,
    round((3.5 + (id % 15)::numeric / 10)::numeric, 2),
    10 + (id % 500),
    'active',
    CASE WHEN id % 4 = 0 THEN NULL ELSE perf_vec(id + 20000) END,
    now() - ((1000 - id) || ' minutes')::interval,
    now()
FROM generate_series(1, 1000) AS id;

INSERT INTO gym_grades (
    id,
    gym_id,
    label,
    difficulty_order,
    is_active,
    created_at,
    updated_at
)
SELECT
    (gym_id - 1) * 3 + grade_no,
    gym_id,
    CASE grade_no WHEN 1 THEN 'V0' WHEN 2 THEN 'V1' ELSE 'V2' END,
    grade_no * 10,
    true,
    now(),
    now()
FROM generate_series(1, 1000) AS gym_id
CROSS JOIN generate_series(1, 3) AS grade_no;

INSERT INTO videos (
    id,
    user_id,
    gym_id,
    gym_grade_id,
    title,
    description,
    gcs_path,
    gcs_streaming_path,
    thumbnail_path,
    duration_seconds,
    recorded_date,
    file_size_bytes,
    mime_type,
    view_count,
    like_count,
    comment_count,
    status,
    is_public,
    created_at,
    updated_at
)
SELECT
    id,
    ((id * 17) % 10000) + 1 AS user_id,
    ((id * 13) % 1000) + 1 AS gym_id,
    (((id * 13) % 1000) * 3) + ((id % 3) + 1) AS gym_grade_id,
    format('Perf climbing clip %s', id),
    'performance seed video',
    format('videos/perf/%s.mp4', id),
    format('videos/perf/%s-stream.mp4', id),
    format('videos/perf/%s-thumb.jpg', id),
    20 + (id % 40),
    current_date - ((id % 365)::int),
    2000000 + (id % 50000000),
    'video/mp4',
    id % 1000,
    0,
    0,
    'done',
    true,
    now() - ((100000 - id) || ' seconds')::interval,
    now()
FROM generate_series(1, 100000) AS id;

INSERT INTO follows (follower_id, following_id, created_at)
SELECT
    follower_id,
    following_id,
    now() - ((follower_id + step_no) || ' seconds')::interval
FROM (
    SELECT
        follower_id,
        ((follower_id + step_no * 97 - 1) % 10000) + 1 AS following_id,
        step_no
    FROM generate_series(1, 10000) AS follower_id
    CROSS JOIN generate_series(1, 30) AS step_no
) AS candidates
WHERE follower_id <> following_id
ON CONFLICT DO NOTHING;

INSERT INTO user_blocks (blocker_id, blocked_id, reason, created_at)
SELECT
    blocker_id,
    blocked_id,
    'perf block graph',
    now()
FROM (
    SELECT
        blocker_id,
        ((blocker_id + step_no * 131 - 1) % 10000) + 1 AS blocked_id
    FROM generate_series(50, 10000, 50) AS blocker_id
    CROSS JOIN generate_series(1, 5) AS step_no
) AS candidates
WHERE blocker_id <> blocked_id
ON CONFLICT DO NOTHING;

INSERT INTO likes (user_id, video_id, created_at)
SELECT
    ((id * 19) % 10000) + 1,
    id,
    now() - ((id % 5000) || ' seconds')::interval
FROM generate_series(1, 20000) AS id
ON CONFLICT DO NOTHING;

INSERT INTO comments (user_id, video_id, content, created_at, updated_at)
SELECT
    ((id * 23) % 10000) + 1,
    ((id * 29) % 100000) + 1,
    format('perf comment %s', id),
    now() - ((id % 3000) || ' seconds')::interval,
    now()
FROM generate_series(1, 10000) AS id;

INSERT INTO analysis_video_results (
    video_id,
    model_version,
    ai_techniques,
    ai_is_dynamic,
    ai_dynamic_probability,
    final_techniques,
    final_is_dynamic,
    feedback_applied,
    created_at,
    updated_at
)
SELECT
    id,
    'rule_v3',
    '["highstep","flagging"]'::jsonb,
    id % 2 = 0,
    CASE WHEN id % 2 = 0 THEN 0.72 ELSE 0.28 END,
    '["highstep","flagging"]'::jsonb,
    id % 2 = 0,
    false,
    now(),
    now()
FROM generate_series(1, 5000) AS id;

UPDATE videos v
SET like_count = counts.like_count
FROM (
    SELECT video_id, count(*)::int AS like_count
    FROM likes
    GROUP BY video_id
) AS counts
WHERE counts.video_id = v.id;

UPDATE videos v
SET comment_count = counts.comment_count
FROM (
    SELECT video_id, count(*)::int AS comment_count
    FROM comments
    GROUP BY video_id
) AS counts
WHERE counts.video_id = v.id;

SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT max(id) FROM users));
SELECT setval(pg_get_serial_sequence('gyms', 'id'), (SELECT max(id) FROM gyms));
SELECT setval(pg_get_serial_sequence('gym_grades', 'id'), (SELECT max(id) FROM gym_grades));
SELECT setval(pg_get_serial_sequence('videos', 'id'), (SELECT max(id) FROM videos));
SELECT setval(pg_get_serial_sequence('follows', 'id'), (SELECT max(id) FROM follows));
SELECT setval(pg_get_serial_sequence('user_blocks', 'id'), (SELECT max(id) FROM user_blocks));
SELECT setval(pg_get_serial_sequence('likes', 'id'), (SELECT max(id) FROM likes));
SELECT setval(pg_get_serial_sequence('comments', 'id'), (SELECT max(id) FROM comments));

ANALYZE;

SELECT
    (SELECT count(*) FROM users) AS users,
    (SELECT count(*) FROM gyms) AS gyms,
    (SELECT count(*) FROM videos) AS videos,
    (SELECT count(*) FROM follows) AS follows,
    (SELECT count(*) FROM user_blocks) AS user_blocks,
    (SELECT count(*) FROM likes) AS likes,
    (SELECT count(*) FROM comments) AS comments,
    (SELECT count(*) FROM analysis_video_results) AS analysis_video_results;
