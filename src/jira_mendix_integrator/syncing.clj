(ns jira-mendix-integrator.syncing)

(defmulti infer-entity-sync-command
  "Receives a Jira entity (:type keyword? :data any?), a Mendix entity (possibly nil) and returns a sync action (create, update, delete)"
  (fn [{:keys [type]}] type))

(defmethod infer-entity-sync-command
  ::jira-story
  [{jira-summary :summary
    jira-description :description
    jira-sprint-id :sprint-id}
   mendix-story]
  (cond
    (nil? mendix-story)
    {:command :integration.mendix/create-story
     :data {:name jira-summary
            :description jira-description
            :type :feature
            :jira-sprint-id jira-sprint-id}}))

