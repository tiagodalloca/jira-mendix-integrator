(ns user.db-connection    
  (:require [next.jdbc :as jdbc]))

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

