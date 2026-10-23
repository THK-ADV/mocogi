-- !Ups
CREATE TABLE modules.webhook_inbox (
  event_id uuid PRIMARY KEY,
  endpoint text NOT NULL,
  payload jsonb NOT NULL,
  received_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- !Downs
DROP TABLE modules.webhook_inbox;
