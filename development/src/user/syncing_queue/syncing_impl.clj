(ns user.syncing-queue.syncing-impl
  (:require [jira-mendix-integrator.syncing
             :refer [syncing-command syncing-interceptor]]
            [jira-mendix-integrator.integrations.mendix :as mendix-integration]
            [jira-mendix-integrator.integrations.helpers :refer [http-request]]))

(defmethod syncing-interceptor [::impl
                                :local/jira-sprint-id-to-mendix]
  ([_ k]
   {:enter (fn [{{:keys [jira-sprint-id dest]} :local/jira-sprint-id-to-mendix
                :as context}]
             (assoc-in context dest "4909319"))}))

(defmethod syncing-interceptor [::impl
                                :local/persist]
  ([_ _]
   {:leave #(doto % prn)}))

(defmethod syncing-command [::impl
                            :mendix/create-story]
  ([_ _ {{:keys [name description type sprint-id] :as command-data} :command-data
         :keys [deps]}]
   (let [request (mendix-integration/create-story name description type sprint-id deps)
         response (http-request request)]
     response)))

