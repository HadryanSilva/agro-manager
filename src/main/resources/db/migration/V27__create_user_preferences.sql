CREATE TABLE user_preferences (
  id                       UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id                  UUID      NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  notification_days_ahead  INT       NOT NULL DEFAULT 7,
  created_at               TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at               TIMESTAMP NOT NULL DEFAULT NOW()
);
