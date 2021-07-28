(ns user.exps.sprint-2
  (:require [integrant.core :as ig]
            [integrant.repl :refer [clear go halt init prep reset reset-all]]
            [jira-mendix-integrator.http.handler :as http-handler]
            [jira-mendix-integrator.http.server :as http-server]
            [paos.service :as paos-service]
            [paos.wsdl :as wsdl]))

(comment
  (clojure.tools.namespace.repl/refresh))

(def config
  {::http-server {:opts {:port 8989}
                  :handler (ig/ref ::handler)}

   ::handler {}

   ::mendix-integration
   {:api-key "3e84ca42-536b-4cdb-bf7a-15f706263a64"
    :wsdl "https://docs.mendix.com/apidocs-mxsdk/apidocs/attachments/9535497/19398865.wsdl"
    :soap-service nil}})

(integrant.repl/set-prep! (constantly config))

(defmethod ig/init-key ::http-server
  [_ {:keys [handler]
      {:keys [port]} :opts}]
  (http-server/start-server {:handler handler :port port}))

(defmethod ig/halt-key! ::http-server
  [_ http-server]
  (when http-server (.stop http-server)))

(defmethod ig/init-key ::handler
  [_ deps]
  (http-handler/get-handler deps))

(defonce mendix-integration (atom nil))

(defmethod ig/init-key ::mendix-integration
  [_ {:keys [wsdl] :as config}]
  (if-not @mendix-integration
    (let [soap-service (wsdl/parse wsdl)
          config (assoc config :soap-service soap-service)]
      (reset! mendix-integration config))
    @mendix-integration))

(comment
  (require '[clj-http.client :as http]
           '[clojure.data.json :as json])

  (http/post
   ))
