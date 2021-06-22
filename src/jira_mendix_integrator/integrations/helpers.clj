(ns jira-mendix-integrator.integrations.helpers
  (:require [clj-http.client :as http-client]
            [clj-http.util :as http-util]))

(defn http-request
  [{:keys [response-fn
           error-fn]
    :as request}]
  (let [clj-http-request (select-keys
                          request
                          [:url
                           :method
                           :content-type
                           :headers
                           :body])]
    (try
      (-> (http-client/request clj-http-request)
          response-fn)
      (catch Exception e
        (if error-fn
          (error-fn e)
          (throw e))))))

(defn encode-query-params
  [params]
  (let [t (comp (filter (fn [[_ v]] v))
                (map (fn [[k v]]
                       (str k "="
                            (cond
                              (fn? v) (v)
                              :else
                              (http-util/url-encode v)))))
                (interpose "&"))]
    (reduce str (into (list) t params))))

(comment
  (encode-query-params (sorted-map "a" "tiago dall'oca"
                                   "auth" "https://roberto.com"
                                   "something" (constantly "something%3Asomething")
                                   "not-something" nil)))

