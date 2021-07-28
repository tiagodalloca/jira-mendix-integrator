(ns jira-mendix-integrator.http.handler
  (:require
   [jira-mendix-integrator.http.mendix.rest-interface.handler
    :as mendix-rest-handler]
   [malli.util :as mu]
   [muuntaja.core :as m]
   reitit.coercion.malli
   [reitit.ring :as ring]
   [reitit.http :as http]
   [reitit.ring.coercion :as coercion]
   reitit.ring.malli
   [reitit.ring.middleware.exception :as exception]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger-ui :as swagger-ui]
   [clojure.data.json :as json]))

(defn- get-routes [deps]
  ["/auth"
   ["/jira-oauth-redirect"
    {:get
     {:handler (fn [request]
                 {:body
                  (select-keys
                   request
                   [:uri :query-params :body-params :headers])})}}]])

(def ^:private options
  {:data
   {:coercion
    (reitit.coercion.malli/create
     {:error-keys
      #{:type :coercion :in :schema :value :errors :humanized :transformed}
      :compile mu/open-schema      
      :strip-extra-keys false
      :default-values true
      :options nil})
    
    :muuntaja m/instance
    :middleware [parameters/parameters-middleware
                 muuntaja/format-negotiate-middleware
                 muuntaja/format-response-middleware
                 (exception/create-exception-middleware
                  (merge
                   exception/default-handlers
                   {clojure.lang.ExceptionInfo
                    (fn [ex request]
                      {:status 500
                       :body (ex-data ex)})
                    :exception/wrap (fn [handler e request]
                                      (.printStackTrace e)
                                      (handler e request))}))
                 muuntaja/format-request-middleware
                 coercion/coerce-request-middleware
                 multipart/multipart-middleware]}})

(defn get-handler [deps]
  (let [routes (get-routes deps)
        handler (ring/ring-handler
                 (ring/router routes options))]
    (ring/routes
     handler
     (mendix-rest-handler/get-handler "/mendix-rest-interface" deps)
     (ring/create-default-handler))))

