(ns jira-mendix-integrator.db.core
  (:require [next.jdbc :as jdbc]
            [honey.sql :as sql]
            [honey.sql.helpers :as h]))

(defn execute!
  [conn sqlmap]
  (jdbc/execute!
   conn
   (sql/format sqlmap)
   jdbc/snake-kebab-opts))

(defn query [conn sqlmap]
  (execute! conn sqlmap))

(comment
  (do (require '[user.development.migrations :as m])
      (with-open [conn (jdbc/get-connection m/pg-db-spec)]
        (query conn {:select [:migration-id] :from :migration}))))

(defn insert-into!
  ([conn table value]
   (execute!
    conn
    {:insert-into [table]
     :values [value]}))
  ([conn table value & values]
   (execute!
    conn
    {:insert-into [table]
     :values (into [value] values)})))

(comment
  (-> {:set {:a 2 :b 3}
       :where '[and [= a 1] [= b 2]]}
      (assoc :update :table)
      sql/format))

(defn update!
  [conn table set-where-map]
  (execute!
   conn
   (assoc set-where-map :update table)))

(defn delete-from!
  [conn table where-map]
  (execute!
   conn
   (assoc where-map :delete-from table)))

(defn insert-or-update!
  [conn table key value]
  (let [key-val (get value key)
        r (execute! conn {:select key :from [table] :where [:= key key-val]})]
    (if (or (nil? r) (empty? r))
      (insert-into! conn table value)
      (update! conn table {:set value
                           :where [:= key key-val]}))))
