(ns jira-mendix-integrator.integrations.jira
  (:require [clojure.data.json :as json]
            [jira-mendix-integrator.integrations.helpers :refer [encode-query-params]]))

(defn auth-redirect-url
  [{:keys [url
           audience
           client-id
           scope
           redirect-uri
           state
           response-type
           prompt]
    :or {url "https://auth.atlassian.com/authorize"
         audience "api.atlassian.com"
         client-id nil
         scope "read:jira-user read:jira-work"
         redirect-uri nil
         state nil
         response-type "code"
         prompt "consent"}}]
  (str
   url
   "?"
   (encode-query-params
    {"audience" audience
     "client_id" client-id
     "scope" scope
     "redirect_uri" redirect-uri
     "state" state
     "response_type" response-type
     "prompt" prompt})))

(defn exchange-auth-code-for-access-token-request
  [code client-id client-secret {:keys [url redirect-uri]}]
  {:url url
   :method :post
   :headers {:content-type "application/json"}
   :body (json/write-str {"grant_type" "authorization_code",
                          "client_id" client-id,
                          "client_secret" client-secret,
                          "code" code,
                          "redirect_uri" redirect-uri})
   :response-fn #(-> % :body (json/read-str) (get "access_token"))})

(defn get-cloud-id-request
  [access-token {:keys [url]}]
  {:url url
   :method :get
   :headers {"Authorization" (str "Bearer " access-token)
             :content-type "application/json"}
   :response-fn #(-> % :body (json/read-str) (nth 0) (get "id"))})

(defn parse-description
  [description]
  (let [content (get description "content")
        paragraph? #(-> % (get "type") (= "paragraph"))
        text? #(-> % (get "text"))
        reduce-content-text (fn [s t]
                              (if-let [text (text? t)]
                                (reduced text)
                                s))
        get-text (fn [content] (reduce reduce-content-text "" content))]
    (->> content
         (filter paragraph?)
         (map #(-> % (get "content") get-text))
         (clojure.string/join "\n\n"))))

(comment
  (parse-description
   {"version" 1,
    "type" "doc",
    "content"
    [{"type" "paragraph",
      "content" [{"type" "text", "text" "vai até com uma descrição "}]}
     {"type" "paragraph",
      "content" [{"type" "text", "text" "oh a descrição ae"}]}]}))

(defn parse-issue
  [{id "id" key "key"
    {summary "summary" description "description"} "fields"
    :as issue}]
  {:type :integrations.jira.entities/story
   :id id
   :key key
   :summary summary
   :description (parse-description description)
   :sprint-id (get-in issue ["fields" "customfield_10020" 0 "id"])})

(defn search-jql-request
  [jql {:keys [access-token cloud-id]} {:keys [url]}]
  {:url (str url cloud-id "/rest/api/3/search/")
   :method :post
   :headers {"Authorization" (str "Bearer " access-token)
             :content-type "application/json"}
   :body (json/write-str {"jql" jql})
   :response-fn #(-> % :body (json/read-str) (get "issues")
                     (->> (map parse-issue)))
   :error-fn
   nil
   ;; #(if (= (:status (ex-data %)) 404)
   ;;    nil
   ;;    (throw %))
   })

(defn integration-instance
  [instance-name]
  {:name instance-name
   :state instance-name
   :cloud-id nil
   :code nil
   :access-token nil})

