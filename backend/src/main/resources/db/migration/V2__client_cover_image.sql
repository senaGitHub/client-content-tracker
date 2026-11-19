-- Optional cover photo per client: a path to one of the bundled images in the frontend
-- (e.g. /images/covers/palm-pool.jpg). Nullable so existing rows stay valid.
alter table client add column cover_image_url varchar(255);
