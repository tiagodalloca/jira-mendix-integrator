(ns user.development.migrations
  (:require [jira-mendix-integrator.migrations.core :as migrations]
            [ragtime.core :as ragtime]
            [next.jdbc :as jdbc]))

(def pg-db-spec
  {:dbtype "postgres"
   :dbname "integrator_db"
   :host "0.0.0.0"
   :port 5431
   :user "integrator"
   :password "postgres"})

(def migrations-dir "development/migrations/")

(comment (migrations/list-resource-dir migrations-dir))

(defn migrate
  []
  (with-open [conn (jdbc/get-connection pg-db-spec)]
    (let [data-store (migrations/data-store-from-connectable conn)]
      (migrations/migrate migrations-dir data-store))))

(defn migrate-rebase
  []
  (with-open [conn (jdbc/get-connection pg-db-spec)]
    (let [data-store (migrations/data-store-from-connectable conn)]
      (migrations/migrate migrations-dir data-store
                          {:strategy ragtime.strategy/rebase}))))

(comment (migrate-rebase))

(defn applied-migrations
  []
  (with-open [conn (jdbc/get-connection pg-db-spec)]
    (jdbc/execute! conn ["SELECT * FROM migration"] jdbc/snake-kebab-opts)))

(defn delete-migration
  [migration-id]
  (with-open [conn (jdbc/get-connection pg-db-spec)]
    (jdbc/execute! conn ["DELETE FROM migration WHERE migration_id = ?"
                         migration-id])))

(comment
  (-> (migrations/read-migrations migrations-dir)
      (ragtime/into-index)
      (doto clojure.tools.namespace.repl))

  (with-open [conn (jdbc/get-connection pg-db-spec)]
    (let [data-store (migrations/data-store-from-connectable conn)
          applied (ragtime.protocols/applied-migration-ids data-store)]
      (prn applied)
      (comment (-> (ragtime applied (map ragtime.protocols/id migrations)))))))

