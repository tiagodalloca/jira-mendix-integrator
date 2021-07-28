(ns jira-mendix-integrator.http.server
  (:require [ring.adapter.jetty :as jetty]))

(defn start-server [{:keys [handler port] :as deps}]
  (jetty/run-jetty handler {:port port :join? false}))

