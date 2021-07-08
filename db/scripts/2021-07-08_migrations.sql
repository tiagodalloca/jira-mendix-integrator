CREATE TABLE IF NOT EXISTS migration (
  id serial PRIMARY KEY,
  migration_id varchar(200) NOT NULL,
  timestamp timestamp default current_timestamp NOT NULL
);
