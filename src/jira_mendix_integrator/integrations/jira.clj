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

(defn search-jql-request
  [jql {:keys [access-token cloud-id]} {:keys [url]}]
  {:url (str url cloud-id "/rest/api/3/search/")
   :method :get
   :headers {"Authorization" (str "Bearer " access-token)
             :content-type "application/json"}
   :query-params {"jql" jql}
   :response-fn #(-> % :body (json/read-str))})

(defn integration-instance
  [instance-name]
  {:name instance-name
   :state instance-name
   :cloud-id nil
   :code nil
   :access-token nil})

