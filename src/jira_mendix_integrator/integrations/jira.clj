(ns jira-mendix-integrator.integrations.jira
  (:require [clojure.data.json :as json]))

(defn get-cloudid-request
  [access-token {:keys [oauth-token-accessible-resources-url]}]
  {:url oauth-token-accessible-resources-url
   :method :get
   :headers {"Authorization" (str "Bearer " access-token)
             :content-type "application/json"}
   :response-fn #(-> % :body (json/read-str) (nth 0) (get "id"))})

(defn exchange-auth-code-for-access-token-request
  [code client-id client-secret {:keys [oauth-token-url redirect-uri]}]
  {:url oauth-token-url
   :method :post
   :headers {:content-type "application/json"}
   :body {:json {"grant_type" "authorization_code",
                 "client_id" client-id,
                 "client_secret" client-secret,
                 "code" code,
                 "redirect_uri" redirect-uri}}
   :reponse-fn #(-> % :body (json/read-str) (get "access_token"))})

(defn search-jql-request
  [jql access-token cloudid {:keys [api-url]}]
  {:url (str api-url cloudid "/rest/api/3/search/")
   :method :get
   :headers {"Authorization" (str "Bearer " access-token)
             :content-type "application/json"}
   :query-params {"jql" jql}
   :response-fn #(-> % :body (json/read-str))})

