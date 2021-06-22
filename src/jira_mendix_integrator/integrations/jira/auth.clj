(ns jira-mendix-integrator.integrations.jira.auth
  (:require [clojure.data.json :as json]
            [jira-mendix-integrator.integrations.helpers :refer [encode-query-params]]))

(defn jira-auth-redirect-url
  [atlassian-auth-url
   {:keys [audience
           client-id
           scope
           redirect-uri
           state
           response-type
           prompt]
    :or {audience "api.atlassian.com"
         client-id nil
         scope "read:jira-user read:jira-work"
         redirect-uri nil
         state nil
         response-type "code"
         prompt "consent"}}]
  (str
   atlassian-auth-url
   "?"
   (encode-query-params
    {"audience" audience
     "client_id" client-id
     "scope" scope
     "redirect_uri" redirect-uri
     "state" state
     "reponse_type" response-type
     "prompt" prompt})))

(defn auth-code-exchange-request
  ([code client-id client-secret redirect-uri]
   {:url
    "https://auth.atlassian.com/oauth/token"
    :method :post
    :headers {:content-type "application/json"}
    :body (json/write-str {"grant_type" "authorization_code",
                           "client_id" client-id,
                           "client_secret" client-secret,
                           "code" code,
                           "redirect_uri" redirect-uri})
    :response-fn #(-> % :body (json/read-str) (get "access_token"))}))

(defn get-cloudid-request
  [access-token]
  {:url
   "https://api.atlassian.com/oauth/token/accessible-resources"
   :method :get
   :headers {"Authorization" (str "Bearer " access-token)
             :content-type "application/json"}
   :reponse-fn #(-> % :body (json/read-str) (nth 0) (get "id"))})

