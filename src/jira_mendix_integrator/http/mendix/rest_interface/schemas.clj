(ns jira-mendix-integrator.http.mendix.rest-interface.schemas
  (:require [malli.core :as m]
            [malli.util :as mu]))

(def story-status
  [:enum :open :started :done])

(def story-type
  [:enum :feature :bug])

(def story-points
  [:enum "_1" "_2" "_3" "_5" "_8" "_13" "_20" "_40" "_100"])

(def story
  [:map
   [:story-id string?]
   [:name string?]
   [:description string?]
   [:status story-status]
   [:story-type story-type]
   [:points story-points]
   [:parent-sprint-id string?]
   [:project-id string?]])

(def story-post
  (-> story
      (mu/select-keys
       [:name
        :description
        :status
        :story-type
        :points
        :parent-sprint-id
        :project-id])
      (mu/update :points mu/update-properties assoc :optinal true)))

(def story-put
  (-> story
      (mu/select-keys
       [:name
        :description
        :status
        :story-type
        :points
        :parent-sprint-id
        :project-id])
      (mu/update :points mu/update-properties assoc :optinal true)))

(comment
  (require  ['malli.generator :as 'mg])
  (mg/generate story-post))

;; REQUESTS

(def mendix-api-header
  [:map
   [:api-key string?]])
