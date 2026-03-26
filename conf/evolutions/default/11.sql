-- !Ups
CREATE TABLE user_settings(
  username text NOT NULL PRIMARY KEY,
  settings jsonb NOT NULL DEFAULT '{}'::jsonb,
  CONSTRAINT user_settings_settings_is_object CHECK (jsonb_typeof(settings) = 'object')
);

-- !Downs
DROP TABLE user_settings;