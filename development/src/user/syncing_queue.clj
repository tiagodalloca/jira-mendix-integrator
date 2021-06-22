(ns user.syncing-queue
  (:require [jira-mendix-integrator.server :as server]
            [jira-mendix-integrator.server.handler :as server-handler]
            [clj-http.client :as http-client]
            [clojure.data.json :as json]
            ring.util.codec
            [paos.wsdl :as wsdl]
            [paos.service :as paos-service]
            clojure.string
            
            [integrant.core :as ig]
            [integrant.repl :refer [clear go halt init prep reset reset-all]])
  (:import [java.util.concurrent ArrayBlockingQueue]))

(declare syncer-handler)

(def config
  {::syncing-queue {:capacity 4}
   ::syncer {:syncing-queue (ig/ref ::syncing-queue)
             :handler #'syncer-handler}})

(integrant.repl/set-prep! (constantly config))

(defonce syncing-queue (atom nil))

(defmethod ig/init-key ::syncing-queue
  [_ {:keys [capacity]}]
  (let [queue (ArrayBlockingQueue. capacity)]
    (reset! syncing-queue queue)
    queue))

(defmethod ig/halt-key! ::syncing-queue
  [_ syncing-queue]
  (.put syncing-queue ::stop))

(defmethod ig/init-key ::syncer
  [_ {:keys [syncing-queue handler]}]
  (future
    (loop [top (.take syncing-queue)]      
      (when-not (= ::stop top)
        (handler top)
        (recur (.take syncing-queue))))))

(defmethod ig/halt-key! ::syncer
  [_ _]
  nil)

(defn syncer-handler
  [top]
  (prn top))

