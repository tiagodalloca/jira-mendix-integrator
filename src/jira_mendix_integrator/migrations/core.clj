(ns jira-mendix-integrator.migrations.core
  (:require [jira-mendix-integrator.migrations.postgres-impl :refer [create-data-store
                                                                     create-migration]]
            [clojure.java.io :as io]
            [ragtime.core :as ragtime]))

(defn uri-from-resource-file [file]
  (-> file
      io/resource
      .toURI))

(defn list-resource-dir [dir]
  (->> (uri-from-resource-file dir)
       java.io.File. 
       .list
       sort))

(comment
  (list-resource-dir "migrations/"))

(defn slurp-from-resource-dir [dir file-name]
  (-> (str dir file-name)
      uri-from-resource-file
      slurp))

(comment (slurp-from-resource-dir "migrations/" "2021-07-12T1020_test1.sql"))

(defn read-migrations [resource-dir]
  (let [file-names (list-resource-dir resource-dir)]
    (into []
          (comp (map (fn [file-name]
                       [file-name
                        (slurp-from-resource-dir resource-dir file-name)]))
                (map (fn [[_ content :as t]]
                       (assoc t 1 (read-string content))))
                (map (fn [[file-name {:keys [up down]}]]
                       (create-migration file-name up down))))
          file-names)))

(comment
  (-> (read-migrations "migrations/")
      prn))

(defn data-store-from-connectable
  [conn]
  (create-data-store conn))

(defn migrate-new [resource-migrations-dir data-store]
  (let [migrations (read-migrations resource-migrations-dir)
        idx (ragtime/into-index migrations)]
    (ragtime/migrate-all
     data-store
     idx migrations
     {:strategy ragtime.strategy/apply-new})))

(defn migrate-rebase [resource-migrations-dir data-store]
  (let [migrations (read-migrations resource-migrations-dir)
        idx (ragtime/into-index migrations)]
    (ragtime/migrate-all
     data-store
     idx migrations
     {:strategy ragtime.strategy/apply-new})))

