ALTER TABLE place_detail_snapshot
    ALTER COLUMN primary_photo_name TYPE TEXT;

COMMENT ON COLUMN place_detail_snapshot.primary_photo_name IS 'Google Places photo resource name, may exceed 255 chars';
