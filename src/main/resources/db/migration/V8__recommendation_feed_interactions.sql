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
