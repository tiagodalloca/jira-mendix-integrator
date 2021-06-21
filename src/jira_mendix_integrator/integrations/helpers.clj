(ns jira-mendix-integrator.integrations.helpers
  (:require [clj-http.client :as http-client]))

(defn http-request
  [{:keys [response-fn
           error-fn]
    :as request}]
  (let [clj-http-request (select-keys request [:url
                                               :method
                                               :content-type
                                               :headers
                                               :body])]
    (try
      (-> (http-client/request clj-http-request)
          response-fn)
      (catch Exception e
        (error-fn e)))))

