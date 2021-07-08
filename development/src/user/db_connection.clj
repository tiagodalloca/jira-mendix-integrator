(ns user.db-connection
  (:require [next.jdbc :as jdbc]
            [ragtime.core :as ragtime]
            [jira-mendix-integrator.migrations.postgres-impl :refer [create-data-store create-migration]]))

(def pg-db-spec
  {:dbtype "postgres"
   :dbname "integrator_db"
   :host "0.0.0.0"
   :port 5431
   :user "integrator"
   :password "postgres"})

(def datasource (jdbc/get-datasource pg-db-spec))

(comment
  (jdbc/execute! datasource ["SELECT CURRENT_DATE ;"]))

(comment
  (jdbc/execute! datasource ["
CREATE TABLE IF NOT EXISTS migration (
  id serial PRIMARY KEY,
  migration_id varchar(200) NOT NULL,
  timestamp timestamp default current_timestamp NOT NULL
);
"])

  (jdbc/execute! datasource ["SELECT * FROM migration"]))

(def pg-data-store (create-data-store datasource))

(def migrations
  (into []
        (map (fn [{:keys [id up down]}] (create-migration id up down)))
        [{:id "m1"
          :up ["CREATE TABLE IF NOT EXISTS m1 (id serial primary key)"]
          :down ["DROP TABLE m1"]}
         {:id "m2"
          :up ["CREATE TABLE IF NOT EXISTS m2 (id serial primary key)"]
          :down ["DROP TABLE m2"]}
         {:id "m3"
          :up ["CREATE TABLE IF NOT EXISTS m3 (id serial primary key)"]
          :down ["DROP TABLE m3"]}]))

(def idx (ragtime/into-index migrations))

(comment
  (ragtime/migrate-all
   pg-data-store
   idx migrations
   {:strategy ragtime.strategy/apply-new}))

