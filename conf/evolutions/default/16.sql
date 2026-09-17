-- !Ups
CREATE TABLE schedule.booking(
  id uuid PRIMARY KEY,
  kind text NOT NULL CHECK (kind IN ('teaching', 'campus', 'faculty')),
  series_id uuid NOT NULL,
  start timestamptz NOT NULL,
  "end" timestamptz NOT NULL,
  title text NOT NULL CHECK (length(trim(title)) > 0),
  note text NULL,
  created_by text NOT NULL CHECK (length(trim(created_by)) > 0),
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  rooms uuid[] NOT NULL CHECK (cardinality(rooms) >= 1),
  lecturer text[] NOT NULL CHECK (cardinality(lecturer) >= 1),
  module uuid NULL REFERENCES modules.module(id),
  course_type text NULL,
  po jsonb NULL,
  CHECK ("end" > START),
  CHECK ((START AT TIME ZONE 'Europe/Berlin')::date =("end" AT TIME ZONE 'Europe/Berlin')::date),
  CHECK ((kind = 'teaching' AND module IS NOT NULL AND course_type IS NOT NULL AND po IS NOT NULL AND jsonb_typeof(po) = 'array' AND jsonb_array_length(po) >= 1) OR (kind IN ('campus', 'faculty') AND module IS NULL AND course_type IS NULL AND po IS NULL))
);

CREATE INDEX idx_booking_kind_start ON schedule.booking(kind, START);

CREATE INDEX idx_booking_series_id ON schedule.booking(series_id);

-- !Downs
DROP FUNCTION IF EXISTS schedule.get_bookings(uuid[]);

DROP FUNCTION IF EXISTS schedule.get_bookings(text, timestamptz, timestamptz);

DROP FUNCTION IF EXISTS schedule.get_semester_plan(date, date);

DROP TABLE schedule.booking;

