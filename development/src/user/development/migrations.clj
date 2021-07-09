(ns user.development.migrations
  (:require [jira-mendix-integrator.migrations.core :as migrations]
            [next.jdbc :as jdbc]))

(def pg-db-spec
  {:dbtype "postgres"
   :dbname "integrator_db"
   :host "0.0.0.0"
   :port 5431
   :user "integrator"
   :password "postgres"})

(def migrations-dir "migrations/")

(defn migrate
  []
  (with-open [conn (jdbc/get-connection pg-db-spec)]
    (let [data-store (migrations/data-store-from-connectable conn)]
      (migrations/migrate-new migrations-dir data-store))))

(defn migrate
  []
  (with-open [conn (jdbc/get-connection pg-db-spec)]
    (let [data-store (migrations/data-store-from-connectable conn)]
      (migrations/migrate-new migrations-dir data-store))))

