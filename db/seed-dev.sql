-- =====================================================================
-- 개발용 더미 데이터 시드 (수동 실행 전용)
-- =====================================================================
-- 실행: IntelliJ DB 콘솔(또는 psql)에서 dev DB(hola-climbing-server)에 대고 실행.
-- 운영/테스트와 무관. 자동 실행되지 않는다.
--
-- 계정 5개 (전부 email_verified=true), 공통 비밀번호: Test1234!
--   a@hola.test ~ e@hola.test
-- 비밀번호 해시는 앱의 BCryptPasswordEncoder(strength 12)로 생성·검증됨.
--
-- 모든 행은 9000번대 고정 ID + ON CONFLICT 처리 → 여러 번 실행해도 안전(멱등).
-- climbed_on/created_at은 CURRENT_DATE 기준 상대값이라 항상 "최근" 데이터.
--
-- 한계: gcs_path가 더미라 영상 재생은 안 됨(피드·목록·상호작용은 정상).
-- =====================================================================

BEGIN;

-- 공통 비밀번호 해시 (= "Test1234!")
-- $2a$12$UOfoguzZ7RyDYmrWdJWYQuvrWBdSaYBGutituxox7mOLOAHV5Kc32

-- ---------- 1) 사용자 ----------
INSERT INTO users (id, email, password_hash, email_verified, nickname, bio) VALUES
(9001, 'a@hola.test', '$2a$12$UOfoguzZ7RyDYmrWdJWYQuvrWBdSaYBGutituxox7mOLOAHV5Kc32', TRUE, 'climber_a', '강남 볼더 주력'),
(9002, 'b@hola.test', '$2a$12$UOfoguzZ7RyDYmrWdJWYQuvrWBdSaYBGutituxox7mOLOAHV5Kc32', TRUE, 'climber_b', '주말 클라이머'),
(9003, 'c@hola.test', '$2a$12$UOfoguzZ7RyDYmrWdJWYQuvrWBdSaYBGutituxox7mOLOAHV5Kc32', TRUE, 'climber_c', '다이노 연습 중'),
(9004, 'd@hola.test', '$2a$12$UOfoguzZ7RyDYmrWdJWYQuvrWBdSaYBGutituxox7mOLOAHV5Kc32', TRUE, 'climber_d', '슬랩 러버'),
(9005, 'e@hola.test', '$2a$12$UOfoguzZ7RyDYmrWdJWYQuvrWBdSaYBGutituxox7mOLOAHV5Kc32', TRUE, 'climber_e', '초보 탈출 도전')
ON CONFLICT DO NOTHING;

-- ---------- 2) 암장 (FK 대상) ----------
INSERT INTO gyms (id, name, address, lat, lng, region_code, status, created_by, business_hours) VALUES
(9001, '손상원클라이밍 강남', '서울 서초구 강남대로 331 지하1층',     37.4916, 127.0300, 'seoul', 'active', 9001, NULL),
(9002, '더클라임 신림',       '서울 관악구 신원로 35 3층',            37.4826, 126.9295, 'seoul', 'active', 9002, NULL),
(9003, '클라이밍파크 강남',   '서울 강남구 강남대로 364 미왕빌딩 9층', 37.4960, 127.0280, 'seoul', 'active', 9003, NULL)
ON CONFLICT (id) DO UPDATE SET
    name           = EXCLUDED.name,
    address        = EXCLUDED.address,
    lat            = EXCLUDED.lat,
    lng            = EXCLUDED.lng,
    region_code    = EXCLUDED.region_code,
    status         = EXCLUDED.status,
    created_by     = EXCLUDED.created_by,
    business_hours = EXCLUDED.business_hours,
    updated_at     = NOW();

-- ---------- 2-1) 암장별 난이도 ----------
-- 실제 지점 공개 후기의 색상 순서를 낮은 난이도 → 높은 난이도 순서로 반영한다.
INSERT INTO gym_grades (id, gym_id, label, difficulty_order, is_active) VALUES
(9014, 9001, '흰색', 10, TRUE),
(9015, 9001, '노랑', 20, TRUE),
(9001, 9001, '초록', 30, TRUE),
(9002, 9001, '파랑', 40, TRUE),
(9003, 9001, '빨강', 50, TRUE),
(9016, 9001, '검정', 60, TRUE),
(9017, 9001, '회색', 70, TRUE),
(9018, 9001, '갈색', 80, TRUE),
(9019, 9001, '핑크', 90, TRUE),
(9020, 9002, '흰색', 10, TRUE),
(9004, 9002, '노랑', 20, TRUE),
(9021, 9002, '주황', 30, TRUE),
(9022, 9002, '초록', 40, TRUE),
(9023, 9002, '파랑', 50, TRUE),
(9024, 9002, '빨강', 60, TRUE),
(9005, 9002, '보라', 70, TRUE),
(9006, 9002, '회색', 80, TRUE),
(9025, 9002, '갈색', 90, TRUE),
(9026, 9002, '검정', 100, TRUE),
(9007, 9003, '노랑', 10, TRUE),
(9008, 9003, '핑크', 20, TRUE),
(9009, 9003, '파랑', 30, TRUE),
(9027, 9003, '빨강', 40, TRUE),
(9028, 9003, '보라', 50, TRUE),
(9029, 9003, '갈색', 60, TRUE),
(9030, 9003, '회색', 70, TRUE),
(9031, 9003, '검정', 80, TRUE),
(9032, 9003, '흰색', 90, TRUE)
ON CONFLICT (id) DO UPDATE SET
    gym_id           = EXCLUDED.gym_id,
    label            = EXCLUDED.label,
    difficulty_order = EXCLUDED.difficulty_order,
    is_active        = EXCLUDED.is_active,
    updated_at       = NOW();

-- ---------- 3) 클라이밍 기록 (달력) — 계정당 6건 ----------
-- 실행 날짜와 무관하게 "이번 달 3건 + 지난 달 3건"이 항상 채워지도록 날짜를 앵커한다.
--   이번 달:  오늘 / 이달 8일·15일(단, 오늘 넘지 않게 LEAST로 클램프)
--   지난 달:  이달 1일 기준 -4 / -15 / -27 일
INSERT INTO climbing_logs (id, user_id, gym_id, climbed_on, grade_counts, memo) VALUES
(9001, 9001, 9001, CURRENT_DATE,                                                           '{"빨강":4,"파랑":2}'::jsonb, '컨디션 좋음'),
(9002, 9001, 9001, LEAST((date_trunc('month',CURRENT_DATE))::date + 7,  CURRENT_DATE),     '{"초록":3,"노랑":3}'::jsonb, NULL),
(9003, 9001, 9002, LEAST((date_trunc('month',CURRENT_DATE))::date + 14, CURRENT_DATE),     '{"파랑":5}'::jsonb,          '신림 원정'),
(9004, 9001, 9001, (date_trunc('month',CURRENT_DATE))::date - 4,                           '{"빨강":2,"파랑":4,"초록":1}'::jsonb, NULL),
(9005, 9001, 9003, (date_trunc('month',CURRENT_DATE))::date - 15,                          '{"노랑":6}'::jsonb,          '클파 강남 문제'),
(9006, 9001, 9001, (date_trunc('month',CURRENT_DATE))::date - 27,                          '{"빨강":5,"파랑":3}'::jsonb, '베스트 데이'),

(9007, 9002, 9002, CURRENT_DATE,                                                           '{"파랑":3}'::jsonb,          NULL),
(9008, 9002, 9002, LEAST((date_trunc('month',CURRENT_DATE))::date + 7,  CURRENT_DATE),     '{"빨강":1,"파랑":2}'::jsonb, '오랜만'),
(9009, 9002, 9001, LEAST((date_trunc('month',CURRENT_DATE))::date + 14, CURRENT_DATE),     '{"초록":4,"파랑":1}'::jsonb, NULL),
(9010, 9002, 9002, (date_trunc('month',CURRENT_DATE))::date - 4,                           '{"파랑":4}'::jsonb,          NULL),
(9011, 9002, 9003, (date_trunc('month',CURRENT_DATE))::date - 15,                          '{"핑크":2,"빨강":1}'::jsonb, NULL),
(9012, 9002, 9002, (date_trunc('month',CURRENT_DATE))::date - 27,                          '{"파랑":3,"초록":3}'::jsonb, NULL),

(9013, 9003, 9001, CURRENT_DATE,                                                           '{"빨강":6}'::jsonb,          '다이노 성공!'),
(9014, 9003, 9003, LEAST((date_trunc('month',CURRENT_DATE))::date + 7,  CURRENT_DATE),     '{"보라":4,"파랑":1}'::jsonb, NULL),
(9015, 9003, 9001, LEAST((date_trunc('month',CURRENT_DATE))::date + 14, CURRENT_DATE),     '{"파랑":2,"초록":2}'::jsonb, NULL),
(9016, 9003, 9003, (date_trunc('month',CURRENT_DATE))::date - 4,                           '{"빨강":3}'::jsonb,          NULL),
(9017, 9003, 9002, (date_trunc('month',CURRENT_DATE))::date - 15,                          '{"초록":5}'::jsonb,          NULL),
(9018, 9003, 9001, (date_trunc('month',CURRENT_DATE))::date - 27,                          '{"빨강":5,"파랑":2}'::jsonb, NULL),

(9019, 9004, 9002, CURRENT_DATE,                                                           '{"초록":3,"파랑":1}'::jsonb, '슬랩 집중'),
(9020, 9004, 9002, LEAST((date_trunc('month',CURRENT_DATE))::date + 7,  CURRENT_DATE),     '{"초록":4}'::jsonb,          NULL),
(9021, 9004, 9001, LEAST((date_trunc('month',CURRENT_DATE))::date + 14, CURRENT_DATE),     '{"파랑":2}'::jsonb,          NULL),
(9022, 9004, 9003, (date_trunc('month',CURRENT_DATE))::date - 4,                           '{"핑크":3,"빨강":1}'::jsonb, NULL),
(9023, 9004, 9002, (date_trunc('month',CURRENT_DATE))::date - 15,                          '{"초록":5,"파랑":2}'::jsonb, NULL),
(9024, 9004, 9001, (date_trunc('month',CURRENT_DATE))::date - 27,                          '{"파랑":3}'::jsonb,          NULL),

(9025, 9005, 9001, CURRENT_DATE,                                                           '{"초록":2}'::jsonb,          '첫 등반'),
(9026, 9005, 9001, LEAST((date_trunc('month',CURRENT_DATE))::date + 7,  CURRENT_DATE),     '{"초록":3}'::jsonb,          NULL),
(9027, 9005, 9002, LEAST((date_trunc('month',CURRENT_DATE))::date + 14, CURRENT_DATE),     '{"초록":1,"파랑":1}'::jsonb, NULL),
(9028, 9005, 9001, (date_trunc('month',CURRENT_DATE))::date - 4,                           '{"초록":4}'::jsonb,          '조금씩 늘어남'),
(9029, 9005, 9003, (date_trunc('month',CURRENT_DATE))::date - 15,                          '{"핑크":2,"파랑":1}'::jsonb, NULL),
(9030, 9005, 9001, (date_trunc('month',CURRENT_DATE))::date - 27,                          '{"초록":3,"파랑":2}'::jsonb, NULL)
ON CONFLICT (id) DO UPDATE SET
    user_id      = EXCLUDED.user_id,
    gym_id       = EXCLUDED.gym_id,
    climbed_on   = EXCLUDED.climbed_on,
    grade_counts = EXCLUDED.grade_counts,
    memo         = EXCLUDED.memo,
    updated_at   = NOW();

-- ---------- 4) 영상 (피드) — gcs_path는 더미 ----------
INSERT INTO videos (id, user_id, gym_id, gym_grade_id, title, description, gcs_path, duration_seconds, recorded_date,
                    status, is_public, created_at) VALUES
(9001, 9001, 9001, 9003, '손상원 빨강 완등',        '오늘의 베스트', 'videos/seed/v9001.mp4', 42, CURRENT_DATE - 1,  'done', TRUE, NOW() - INTERVAL '1 day'),
(9002, 9001, 9001, 9002, '손상원 파랑 연습',        NULL,           'videos/seed/v9002.mp4', 30, CURRENT_DATE - 5,  'done', TRUE, NOW() - INTERVAL '5 days'),
(9003, 9001, 9002, 9023, '더클라임 신림 파랑 원정', '신림 다녀옴',   'videos/seed/v9003.mp4', 51, CURRENT_DATE - 9,  'done', FALSE, NOW() - INTERVAL '9 days'),
(9004, 9002, 9002, 9024, '신림 빨강 세션',          NULL,           'videos/seed/v9004.mp4', 38, CURRENT_DATE - 2,  'done', TRUE, NOW() - INTERVAL '2 days'),
(9005, 9002, 9002, 9021, '신림 주황 트라이',        NULL,           'videos/seed/v9005.mp4', 27, CURRENT_DATE - 7,  'done', TRUE, NOW() - INTERVAL '7 days'),
(9006, 9003, 9001, 9003, '손상원 다이노 성공',      '드디어!',      'videos/seed/v9006.mp4', 19, CURRENT_DATE - 3,  'done', TRUE, NOW() - INTERVAL '3 days'),
(9007, 9003, 9003, 9028, '클파 강남 보라',          NULL,           'videos/seed/v9007.mp4', 44, CURRENT_DATE - 8,  'done', TRUE, NOW() - INTERVAL '8 days'),
(9008, 9003, 9001, 9003, '손상원 빨강 플래시',      NULL,           'videos/seed/v9008.mp4', 33, CURRENT_DATE - 15, 'done', TRUE, NOW() - INTERVAL '15 days'),
(9009, 9004, 9002, 9022, '신림 초록 집중',          '슬랩 좋아',    'videos/seed/v9009.mp4', 47, CURRENT_DATE - 1,  'done', TRUE, NOW() - INTERVAL '1 day'),
(9010, 9004, 9002, 9021, '신림 주황 마스터',        NULL,           'videos/seed/v9010.mp4', 25, CURRENT_DATE - 6,  'done', TRUE, NOW() - INTERVAL '6 days'),
(9011, 9005, 9001, 9014, '손상원 첫 등반 기록',    '시작!',        'videos/seed/v9011.mp4', 22, CURRENT_DATE - 2,  'done', TRUE, NOW() - INTERVAL '2 days'),
(9012, 9005, 9001, 9001, '손상원 초록 도전',        NULL,           'videos/seed/v9012.mp4', 29, CURRENT_DATE - 5,  'done', TRUE, NOW() - INTERVAL '5 days'),
(9013, 9005, 9002, 9023, '신림 비공개 연습',        '아직 미공개',  'videos/seed/v9013.mp4', 31, CURRENT_DATE - 11, 'done', FALSE, NOW() - INTERVAL '11 days')
ON CONFLICT (id) DO UPDATE SET
    user_id          = EXCLUDED.user_id,
    gym_id           = EXCLUDED.gym_id,
    gym_grade_id     = EXCLUDED.gym_grade_id,
    title            = EXCLUDED.title,
    description      = EXCLUDED.description,
    gcs_path         = EXCLUDED.gcs_path,
    duration_seconds = EXCLUDED.duration_seconds,
    recorded_date    = EXCLUDED.recorded_date,
    status           = EXCLUDED.status,
    is_public        = EXCLUDED.is_public,
    created_at       = EXCLUDED.created_at,
    updated_at       = NOW();

-- ---------- 5) 암장 리뷰 (사용자당 암장 1개 — UNIQUE(gym_id,user_id)) ----------
INSERT INTO gym_reviews (id, gym_id, user_id, rating, content) VALUES
(9001, 9001, 9001, 5, '시설 최고, 문제 다양'),
(9002, 9001, 9002, 4, '주말엔 좀 붐빔'),
(9003, 9001, 9003, 5, '다이노 문제 많아서 좋음'),
(9004, 9002, 9002, 4, '신림역 접근성 굿'),
(9005, 9002, 9004, 5, '슬랩 섹션 훌륭'),
(9006, 9002, 9005, 3, '초보 문제가 적은 편'),
(9007, 9003, 9003, 5, '클파 강남 문제 재미있음'),
(9008, 9003, 9001, 4, '넓고 쾌적')
ON CONFLICT (id) DO UPDATE SET
    gym_id     = EXCLUDED.gym_id,
    user_id    = EXCLUDED.user_id,
    rating     = EXCLUDED.rating,
    content    = EXCLUDED.content,
    updated_at = NOW();

-- ---------- 6) 팔로우 (a가 인기 계정) ----------
INSERT INTO follows (id, follower_id, following_id) VALUES
(9001, 9002, 9001),
(9002, 9003, 9001),
(9003, 9004, 9001),
(9004, 9005, 9001),
(9005, 9001, 9003),
(9006, 9002, 9003),
(9007, 9005, 9004),
(9008, 9001, 9002)
ON CONFLICT DO NOTHING;

-- ---------- 7) 즐겨찾기 ----------
INSERT INTO favorites (id, user_id, gym_id) VALUES
(9001, 9001, 9001),
(9002, 9001, 9003),
(9003, 9002, 9002),
(9004, 9003, 9001),
(9005, 9004, 9002),
(9006, 9005, 9001)
ON CONFLICT DO NOTHING;

-- ---------- 8) 좋아요 ----------
INSERT INTO likes (id, user_id, video_id) VALUES
(9001, 9002, 9001),(9002, 9003, 9001),(9003, 9004, 9001),
(9004, 9001, 9006),(9005, 9002, 9006),(9006, 9005, 9006),
(9007, 9001, 9004),(9008, 9003, 9004),
(9009, 9002, 9007),(9010, 9004, 9007),
(9011, 9001, 9009),(9012, 9005, 9009),
(9013, 9003, 9011),(9014, 9002, 9002)
ON CONFLICT DO NOTHING;

-- ---------- 9) 댓글 ----------
INSERT INTO comments (id, user_id, video_id, content) VALUES
(9001, 9002, 9001, '완등 축하해요!'),
(9002, 9003, 9001, '무브 깔끔하네요'),
(9003, 9001, 9006, '다이노 미쳤다'),
(9004, 9005, 9006, '대박...'),
(9005, 9004, 9004, '주말에 같이 가요'),
(9006, 9001, 9007, '클파 문제 정보 감사'),
(9007, 9003, 9009, '슬랩 폼 좋아요'),
(9008, 9002, 9011, '첫 등반 화이팅!')
ON CONFLICT DO NOTHING;

-- ---------- 10) 비정규화 카운트 동기화 (삽입한 실제 행 기준) ----------
UPDATE videos v SET
    like_count    = (SELECT COUNT(*) FROM likes    l WHERE l.video_id = v.id),
    comment_count = (SELECT COUNT(*) FROM comments c WHERE c.video_id = v.id AND c.deleted_at IS NULL)
WHERE v.id BETWEEN 9001 AND 9013;

UPDATE gyms g SET
    rating_count = (SELECT COUNT(*)            FROM gym_reviews r WHERE r.gym_id = g.id),
    rating_avg   = COALESCE((SELECT ROUND(AVG(r.rating), 2) FROM gym_reviews r WHERE r.gym_id = g.id), 0)
WHERE g.id BETWEEN 9001 AND 9003;

COMMIT;

-- 확인용:
--   SELECT id, email, nickname, email_verified FROM users WHERE id >= 9001;
--   로그인:  POST /api/auth/login  { "email": "a@hola.test", "password": "Test1234!" }
