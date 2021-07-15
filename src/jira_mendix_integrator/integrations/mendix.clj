(ns jira-mendix-integrator.integrations.mendix
  (:require [paos.service :as paos-service]
            [jira-mendix-integrator.integrations.helpers :refer [http-request]]
            clojure.string))

(defn- ->clj-kw
  ([str ns]
   (let [k-name (->> (clojure.string/split str #"(?<=[a-z])(?=[A-Z])")
                     (map clojure.string/lower-case)
                     (clojure.string/join "-"))]
     (if ns
       (keyword ns k-name)
       (keyword k-name))))
  ([str]
   (->clj-kw str nil)))

(defn parse-mendix-obj
  ([s ns]
   (reduce-kv
    (fn [acc k v]
      (assoc acc (->clj-kw k ns) (get v "__value")))
    {}
    s))
  ([s]
   (parse-mendix-obj s nil)))

(defn- parse-sprints [ss]
  (map #(parse-mendix-obj (get % "Sprints") "mendix.sprint") ss))


(comment (parse-sprints [{"Sprints"
                          {"SprintID" {"__value" "4909320"},
                           "EndDate" {"__value" "2021-07-05T02:59:59.000Z"},
                           "IsActiveSprint" {"__value" "true"},
                           "HasStories" {"__value" "true"},
                           "Deleted" {"__value" "false"},
                           "CreationDate" {"__value" "2021-06-21T13:42:24.264Z"},
                           "Completed" {"__value" "false"},
                           "isBacklog" {"__value" "false"},
                           "Name" {"__value" "Get started"},
                           "StartDate" {"__value" "2021-06-21T03:00:00.000Z"}}}
                         {"Sprints"
                          {"SprintID" {"__value" "4909319"},
                           "EndDate" {"__value" nil},
                           "IsActiveSprint" {"__value" "false"},
                           "HasStories" {"__value" "true"},
                           "Deleted" {"__value" "false"},
                           "CreationDate" {"__value" "2021-06-21T13:42:24.189Z"},
                           "Completed" {"__value" "false"},
                           "isBacklog" {"__value" "true"},
                           "Name" {"__value" "Backlog"},
                           "StartDate" {"__value" nil}}}]))

(defn parse-story
  [story]
  (parse-mendix-obj story))

(comment
  (parse-story {"NewStoryID" {"__value" "4933426"}}))

(defn parse-story-type
  [type]
  (or
   ({:feature "Feature"
     :bug "Bug"}
    type)
   "Feature"))

(comment
  (parse-story-type :feature))

(defn parse-story-status
  [status]
  (or
   ({:open "Open"
     :started "Started"
     :done "Done"}
    type)
   "Open"))

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
        parse-fn (partial paos-service/parse-response srv)
        request {:url url
                 :method :post
                 :content-type content-type
                 :headers headers
                 :body body}]
    (merge
     request
     {:response-fn #(-> % :body parse-fn
                        (get-in ["Envelope"
                                 "Body"
                                 "GetSprintsResponse"
                                 "Sprints"])
                        (parse-sprints))
      :error-fn (fn [e]
                  (throw
                   (ex-info (->> e ex-data :body (paos-service/parse-fault srv))
                            {:request request})))})))

(defn create-story
  [name description type sprint-id
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
                      "StoryType" {:__value (parse-story-type type)}
                      "SprintID" {:__value sprint-id}
                      "ProjectID" {:__value "1b474997-f874-41ea-af9e-99acfdfe6935"}
                      "ApiKey" {:__value api-key}}))
        body (paos-service/wrap-body srv context)
        parse-fn (partial paos-service/parse-response srv)
        request {:url url
                 :method :post
                 :content-type content-type
                 :headers headers
                 :body body}]
    (merge
     request
     {:response-fn #(-> % :body parse-fn
                        (get-in ["Envelope"
                                 "Body"
                                 "CreateStoryResponse"])
                        (parse-story))
      :error-fn (fn [e]
                  (throw
                   (try (ex-info (->> e ex-data :body (paos-service/parse-fault srv))
                                 {:request request})
                        (catch Exception _
                          (throw e)))))})))

(defn udpate-story
  [story-id name description status story-type story-points sprint-id
   {:keys [soap-service api-key]}]
  (let [soap-service soap-service
        srv (get-in soap-service ["StoriesAPISoap" :operations "UpdateStory"])
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
                     ["Envelope" "Body" "UpdateStory"]
                     {"StoryID" {:__value story-id}
                      "Name" {:__value name}
                      "Description" {:__value description}
                      "Status" {:__value (parse-story-status status)}
                      "StoryType" {:__value (parse-story-type story-type)}
                      "Points" {:__value story-points}
                      "ParentSprintID" {:__value sprint-id}
                      "ProjectID" {:__value "1b474997-f874-41ea-af9e-99acfdfe6935"}
                      "ApiKey" {:__value api-key}}))
        body (paos-service/wrap-body srv context)
        parse-fn (partial paos-service/parse-response srv)
        request {:url url
                 :method :post
                 :content-type content-type
                 :headers headers
                 :body body}]
    (merge
     request
     {:response-fn #(-> % :body parse-fn
                        (get-in ["Envelope"
                                 "Body"
                                 "UpdateStoryResponse"])
                        (parse-story))
      :error-fn (fn [e]
                  (throw
                   (try (ex-info (->> e ex-data :body (paos-service/parse-fault srv))
                                 {:request request})
                        (catch Exception _
                          (throw e)))))})))

