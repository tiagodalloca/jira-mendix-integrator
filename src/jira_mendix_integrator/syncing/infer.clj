(ns jira-mendix-integrator.syncing.infer)

(defmulti infer-entity-sync-command
  "Receives a Jira entity (:type keyword? :data any?), a Mendix entity (possibly nil) and returns a sync action (create, update, delete)"
  (fn [{:keys [type]} _] type))

(defmethod infer-entity-sync-command
  :jira/story
  [{jira-summary :summary
    jira-description :description
    jira-sprint-id :sprint-id}
   db-jira-story]
  (cond
    (nil? db-jira-story)
    {:interceptors [:local/jira-sprint-id-to-mendix

                    :local/persist

                    {:leave (fn [{{:keys [new-story-id]}
                                 :mendix/create-story
                                 :keys [command-data]
                                 :as context}]
                              (assoc-in context [:local/persist]
                                        (merge
                                         command-data
                                         {:type :mendix/story
                                          :story-id new-story-id})))}]
     :command :mendix/create-story
     :context {:command-data {:name jira-summary
                              :description jira-description
                              :type :feature}
               :local/jira-sprint-id-to-mendix
               {:jira-sprint-id jira-sprint-id
                :dest [:command-data :sprint-id]}}}))

