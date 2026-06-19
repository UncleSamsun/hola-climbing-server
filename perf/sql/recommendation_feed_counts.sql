\set ON_ERROR_STOP on

SELECT current_database() AS database_name, now() AS captured_at;

SELECT 'users' AS relation, count(*) AS rows FROM users
UNION ALL SELECT 'gyms', count(*) FROM gyms
UNION ALL SELECT 'gym_grades', count(*) FROM gym_grades
UNION ALL SELECT 'videos', count(*) FROM videos
UNION ALL SELECT 'follows', count(*) FROM follows
UNION ALL SELECT 'user_blocks', count(*) FROM user_blocks
UNION ALL SELECT 'user_video_interactions', count(*) FROM user_video_interactions
UNION ALL SELECT 'likes', count(*) FROM likes
UNION ALL SELECT 'comments', count(*) FROM comments
UNION ALL SELECT 'analysis_video_results', count(*) FROM analysis_video_results
ORDER BY relation;

SELECT
    schemaname,
    relname,
    pg_size_pretty(pg_total_relation_size(format('%I.%I', schemaname, relname)::regclass)) AS total_size,
    pg_size_pretty(pg_relation_size(format('%I.%I', schemaname, relname)::regclass)) AS table_size,
    pg_size_pretty(pg_indexes_size(format('%I.%I', schemaname, relname)::regclass)) AS indexes_size
FROM pg_stat_user_tables
WHERE relname IN ('users', 'gyms', 'gym_grades', 'videos', 'follows', 'user_blocks', 'user_video_interactions', 'likes', 'comments', 'analysis_video_results')
ORDER BY pg_total_relation_size(format('%I.%I', schemaname, relname)::regclass) DESC;

SELECT
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename IN ('users', 'gyms', 'gym_grades', 'videos', 'follows', 'user_blocks', 'user_video_interactions')
ORDER BY tablename, indexname;
