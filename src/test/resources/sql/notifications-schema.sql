-- Notification 통합 테스트용 notifications 테이블.
-- users-schema.sql 다음에 실행되어야 한다 (FK 대상).
DROP TABLE IF EXISTS notifications CASCADE;

CREATE TABLE notifications (
    id              BIGSERIAL PRIMARY KEY,
    recipient_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sender_id       BIGINT REFERENCES users(id) ON DELETE SET NULL,
    type            VARCHAR(30) NOT NULL,
    target_type     VARCHAR(30),
    target_id       BIGINT,
    title           VARCHAR(200),
    content         TEXT,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
