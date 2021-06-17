(ns user.jira-auth-test
  (:require [jira-mendix-integrator.server :as server]
            [jira-mendix-integrator.server.handler :as server-handler]
            
            [integrant.core :as ig]
            [integrant.repl :refer [clear go halt init prep reset reset-all]]))

(def config
  {::server {:opts {:port 8989}
             :handler (ig/ref ::handler)}

   ::handler {}})

(integrant.repl/set-prep! (constantly config))

(defmethod ig/init-key ::server [_ {:keys [handler]
                                    {:keys [port]} :opts}]
  (server/start-server {:handler handler :port port}))

(defmethod ig/halt-key! ::server [_ server]
  (when server (.stop server)))

(defmethod ig/init-key ::handler [_ deps]
  (server-handler/get-handler deps))

(comment
  (prep)
  (init)
  (clear)
  (reset))

