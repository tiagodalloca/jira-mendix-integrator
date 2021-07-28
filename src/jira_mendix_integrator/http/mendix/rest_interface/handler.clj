(ns jira-mendix-integrator.http.mendix.rest-interface.handler
  (:require [jira-mendix-integrator.http.mendix.rest-interface.schemas :as schemas]
            [jira-mendix-integrator.http.mendix.rest-interface.endpoint-handlers
             :as endpoint-handlers]
            [reitit.ring :as ring]
            [reitit.coercion.malli]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.malli]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.dev.pretty :as pretty]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.adapter.jetty :as jetty]
            [muuntaja.core :as m]
            [clojure.java.io :as io]
            [malli.util :as mu]))

(defn- get-routes-data [path deps]
  [path
   ["/story"
    {:post {
            :parameters {:header schemas/mendix-api-header
                         :body schemas/story-post}
            :responses {200 {:body [:enum "Ok"]}}
            :handler (fn [request]
                       (endpoint-handlers/story-post request deps)
                       {:body "Ok"})}
     
     :put {:parameters {:header schemas/mendix-api-header
                        :body schemas/story-put}
           ;; :responses {200 {:body [:enum "Ok"]}}
           :handler (fn [request] (endpoint-handlers/story-put request deps))}}]
   
   ["/swagger.json"
    {:get {:no-doc true
           :swagger {:info {:title "Mendix API REST interface"
                            :description "with reitit-http"}}
           :handler (swagger/create-swagger-handler)}}]
   ["/asdf"
    {:get
     {:handler (fn [request]
                 {:body "Ok"})}}]])

(defn get-handler
  [path deps]
  (ring/routes
   (ring/ring-handler
    (ring/router
     (get-routes-data path  {})
     {;;:reitit.middleware/transform dev/print-request-diffs ;; pretty diffs
      ;;:validate spec/validate ;; enable spec validation for route data
      ;;:reitit.spec/wrap spell/closed ;; strict top-level validation
      :exception pretty/exception
      :data {:coercion (reitit.coercion.malli/create
                        {;; set of keys to include in error messages
                         :error-keys #{#_:type :coercion :in :schema :value :errors :humanized #_:transformed}
                         ;; schema identity function (default: close all map schemas)
                         :compile mu/closed-schema
                         ;; strip-extra-keys (effects only predefined transformers)
                         :strip-extra-keys true
                         ;; add/set default values
                         :default-values true
                         ;; malli options
                         :options nil})
             :muuntaja m/instance
             :middleware [;; swagger feature
                          swagger/swagger-feature
                          ;; query-params & form-params
                          parameters/parameters-middleware
                          ;; content-negotiation
                          muuntaja/format-negotiate-middleware
                          ;; encoding response body
                          muuntaja/format-response-middleware
                          ;; exception handling
                          exception/exception-middleware
                          ;; decoding request body
                          muuntaja/format-request-middleware
                          ;; coercing response bodys
                          coercion/coerce-response-middleware
                          ;; coercing request parameters
                          coercion/coerce-request-middleware
                          ;; multipart
                          multipart/multipart-middleware]}}))

   (swagger-ui/create-swagger-ui-handler
    {:path path
     :config {:validatorUrl nil
              :operationsSorter "alpha"}})))

