(ns jira-mendix-integrator.integrations.mendix
  (:require [paos.service :as paos-service]))

(defn get-sprints-request
  [{:keys [soap-service api-key]}]
  (let [soap-service soap-service
        srv (get-in soap-service ["StoriesAPISoap" :operations "GetSprints"])
        url (get-in soap-service ["StoriesAPISoap" :url])
        content-type (paos-service/content-type srv)
        headers (paos-service/soap-headers srv)
        srv-mapping (paos-service/request-mapping srv)
        context (-> srv-mapping
                    (assoc-in
                     ["Envelope" "Header" "authentication"]
                     {"username" {:__value "PlatformAPIUser"}
                      "password" {:__value "PlatformAPIPassword"}})
                    (assoc-in
                     ["Envelope" "Body" "GetSprints"]
                     {"ProjectID" {:__value "1b474997-f874-41ea-af9e-99acfdfe6935"}
                      "ApiKey" {:__value api-key}}))
        body (paos-service/wrap-body srv context)
        parse-fn (partial paos-service/parse-response srv)]
    {:url url
     :method :post
     :content-type content-type
     :headers headers
     :body body
     :response-fn #(-> % :body parse-fn)
     :error-fn (fn [e] (->> e ex-data :body (paos-service/parse-fault srv)))}))

(defn create-story
  [{:keys [name description type]}
   sprint-id
   {:keys [soap-service api-key]}]
  (let [soap-service soap-service
        srv (get-in soap-service ["StoriesAPISoap" :operations "CreateStory"])
        url (get-in soap-service ["StoriesAPISoap" :url])
        content-type (paos-service/content-type srv)
        headers (paos-service/soap-headers srv)
        srv-mapping (paos-service/request-mapping srv)
        context (-> srv-mapping
                    (assoc-in
                     ["Envelope" "Header" "authentication"]
                     {"username" {:__value "PlatformAPIUser"}
                      "password" {:__value "PlatformAPIPassword"}})
                    (assoc-in
                     ["Envelope" "Body" "CreateStory"]
                     {"Name" {:__value name}
                      "Description" {:__value description}
                      "StoryType" {:__value (condp type identity
                                              :feature "Feature"
                                              :bug "Bug"
                                              "Feature")}
                      "SprintID" {:__value sprint-id}
                      "ProjectID" {:__value "1b474997-f874-41ea-af9e-99acfdfe6935"}
                      "ApiKey" {:__value api-key}}))
        body (paos-service/wrap-body srv context)
        parse-fn (partial paos-service/parse-response srv)]
    {:url url
     :method :post
     :content-type content-type
     :headers headers
     :body body
     :response-fn #(-> % :body parse-fn)
     :error-fn (fn [e] (->> e ex-data :body (paos-service/parse-fault srv)))}))

