(ns jira-mendix-integrator.http.mendix.rest-interface.endpoint-handlers
  (:require [jira-mendix-integrator.http.mendix.rest-interface.schemas :as schemas]
            [jira-mendix-integrator.integrations.mendix :as mendix]
            [jira-mendix-integrator.integrations.helpers :refer [http-request]]
            [malli.core :as m]))

(defn story-post
  [{{story :body
     {:keys [api-key]} :header} :parameters}
   deps]
  (let [{:keys [name
                description
                status
                story-type
                points
                parent-sprint-id
                project-id]}
        story
        
        request (mendix/create-story
                 name
                 description
                 story-type
                 parent-sprint-id
                 (merge deps {:api-key api-key}))]
    (http-request request)))

(m/=> story-post
      [:=> [:cat 
            [:map [:parameters [:map
                                [:body schemas/story-post]
                                [:header schemas/mendix-api-header]]]]
            [:map [:soap-service some?]]]
       any?])

(defn story-put
  [{{story :body
     {:keys [api-key]} :header} :parameters}
   deps]
  (let [{:keys [name
                description
                status
                story-type
                points
                parent-sprint-id
                project-id]}
        story
        
        request (mendix/udpate-story
                 name
                 description
                 story-type
                 parent-sprint-id
                 (merge deps {:api-key api-key}))]
    (http-request request)))

(m/=> story-put
      [:=> [:cat 
            [:map [:parameters [:map
                                [:body schemas/story-post]
                                [:header schemas/mendix-api-header]]]]
            [:map [:soap-service some?]]]
       any?])

