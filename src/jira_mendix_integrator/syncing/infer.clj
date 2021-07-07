(ns jira-mendix-integrator.syncing.infer)

(defmulti infer-entity-sync-command
  "Receives a Jira entity (:type keyword? :data any?), a Mendix entity (possibly nil) and returns a sync action (create, update, delete)"
  (fn [{:keys [type]} _] type))

(defmethod infer-entity-sync-command
  :integrations.jira.entities/story
  [{jira-summary :summary
    jira-description :description
    jira-sprint-id :sprint-id}
   mendix-story]
  (cond
    (nil? mendix-story)
    {:interceptors [:integrations.local/jira-sprint-id-to-mendix

                    :integrations.local/persist

                    {:leave (fn [{{:keys [new-story-id]}
                                 :integrations.mendix/create-story
                                 :keys [command-data]
                                 :as context}]
                              (assoc-in context [:integrations.local/persist]
                                        (merge
                                         command-data
                                         {:type :integrations.mendix.entities/story
                                          :story-id new-story-id})))}]
     :command :integrations.mendix/create-story
     :context {:command-data {:name jira-summary
                              :description jira-description
                              :type :feature}
               :integrations.local/jira-sprint-id-to-mendix
               {:jira-sprint-id jira-sprint-id
                :dest [:command-data :sprint-id]}}}))

