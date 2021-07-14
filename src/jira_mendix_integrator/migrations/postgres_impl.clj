(ns jira-mendix-integrator.migrations.postgres-impl
  (:require [ragtime.protocols :refer [DataStore Migration]]
            [next.jdbc :as jdbc]))

(defrecord PgDataStore
    [connectable]
  DataStore
  (add-migration-id [store migration-id]
    (jdbc/execute!
     connectable
     ["INSERT INTO migration(migration_id) VALUES (?)"
      migration-id]))
  (remove-migration-id [store migration-id]
    (jdbc/execute!
     connectable
     ["DELETE FROM migration WHERE migration_id = ? "
      migration-id]))
  (applied-migration-ids [store]
    (->> (jdbc/execute!
          connectable
          ["SELECT migration_id FROM migration ORDER BY timestamp DESC"])
         (into (list)
               (map :migration/migration_id)))))


(defn create-data-store
  [connectable]
  (->PgDataStore connectable))

(defrecord PgMigration
    [id up-sql-params down-sql-params]
  Migration
  (id [_] id)
  (run-up! [_ store]
    (let [conn (:connectable store)]
      (jdbc/execute! conn up-sql-params)))
  (run-down! [_ store]
    (let [conn (:connectable store)]
      (jdbc/execute! conn down-sql-params))))

(defn create-migration
  [id up-sql-params down-sql-params]
  (->PgMigration id up-sql-params down-sql-params))

