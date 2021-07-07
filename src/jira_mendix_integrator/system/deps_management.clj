(ns jira-mendix-integrator.system.deps-management
  (:require [jira-mendix-integrator.integrations.jira :as jira-integration]))

(defn init-jira-integration
  [_ {:keys [instances] :as deps}]
  (let [integration-instances
        (into {} (map #(vector % (jira-integration/integration-instance %))) instances)]
    (assoc deps :instances (atom integration-instances))))



