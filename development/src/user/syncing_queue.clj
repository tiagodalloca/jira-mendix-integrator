(ns user.syncing-queue
  (:require [jira-mendix-integrator.server :as server]
            [jira-mendix-integrator.server.handler :as server-handler]
            [jira-mendix-integrator.integrations.jira :as jira-integration]
            [jira-mendix-integrator.integrations.helpers :as integrations-helper]
            
            [integrant.core :as ig]
            [integrant.repl :refer [clear go halt init prep reset reset-all]])
  (:import [java.util.concurrent ArrayBlockingQueue]))

(defn syncer-handler
  [top]
  (prn top))

(def config
  {::syncing-queue {:capacity 4}
   ::syncer {:syncing-queue (ig/ref ::syncing-queue)
             :syncer-handler #'syncer-handler}
   ::server {:opts {:port 8989}
             :handler (ig/ref ::handler)}

   ::handler {}

   ::jira-redirect-uri "https://64a8726ea3bd.ngrok.io/auth/jira-oauth-redirect"

   ::jira-integration
   {:instances #{"clj-repl-dalloca"}
    
    :client-id
    "hR8vp2JYgGUQBItgZ0HuB5JLXoPzpZWa"
    
    :client-secret
    "bCy_P5WKRo_n4OmKo5JrZoRS8TU_3llEHOU2hShTjc--QHEslRUlS8UxDsTWxmYZ"
    
    :auth-redirect
    {:url "https://auth.atlassian.com/authorize"
     :audience "api.atlassian.com"
     :scope (constantly "read%3Ajira-user%20read%3Ajira-work")
     :redirect-uri (ig/ref ::jira-redirect-uri)}

    :auth-code-exchange
    {:url "https://auth.atlassian.com/oauth/token"
     :redirect-uri (ig/ref ::jira-redirect-uri)}

    :get-cloud-id-request
    {:url "https://api.atlassian.com/oauth/token/accessible-resources"}

    :api-request
    {:url "https://api.atlassian.com/ex/jira/"}}})

(integrant.repl/set-prep! (constantly config))

(defonce syncing-queue (atom nil))

(defmethod ig/init-key ::syncing-queue
  [_ {:keys [capacity]}]
  (let [queue (ArrayBlockingQueue. capacity)]
    (reset! syncing-queue queue)
    queue))

(defmethod ig/halt-key! ::syncing-queue
  [_ syncing-queue]
  (.put syncing-queue ::stop))

(defmethod ig/init-key ::syncer
  [_ {:keys [syncing-queue syncer-handler]}]
  (future
    (loop [top (.take syncing-queue)]      
      (when-not (= ::stop top)
        (syncer-handler top)
        (recur (.take syncing-queue))))))

(defmethod ig/halt-key! ::syncer
  [_ _]
  nil)

(defmethod ig/init-key ::server
  [_ {:keys [handler]
      {:keys [port]} :opts}]
  (server/start-server {:handler handler :port port}))

(defmethod ig/halt-key! ::server
  [_ server]
  (when server (.stop server)))

(defmethod ig/init-key ::handler
  [_ deps]
  (server-handler/get-handler deps))

(defonce jira-integration (atom nil))

(defmethod ig/init-key ::jira-redirect-uri [_ uri]  uri)

(defmethod ig/init-key ::jira-integration
  [_ {:keys [instances] :as deps}]
  (let [integration-instances
        (into {} (map #(vector % (jira-integration/integration-instance %))) instances)]
    (reset! jira-integration
            (assoc deps :instances integration-instances))))

(defn runtime-exec
  [command-str]
  (-> (Runtime/getRuntime) (.exec command-str)))

(defn open-auth
  [integration-name]
  (runtime-exec
   (str "powershell.exe Start-Process \""
        (let [integration-map @jira-integration
              client-id (:client-id integration-map)
              instance (get-in integration-map [:instances integration-name])
              state (:state instance)]
          (jira-integration/auth-redirect-url
           (merge (get integration-map :auth-redirect)
                  {:client-id client-id
                   :state state})))
        "\"")))

(defn set-jira-auth-code!
  [integration-name code]
  (swap! jira-integration #(update-in % [:instances integration-name :code]
                                      (constantly code))))

(defn exchange-jira-auth-code!
  [integration-name]
  (let [integration-map @jira-integration
        instance (get-in integration-map [:instances integration-name])
        code (:code instance)
        client-id (:client-id integration-map)
        client-secret (:client-secret integration-map)
        auth-code-exchange (:auth-code-exchange integration-map)
        request (jira-integration/exchange-auth-code-for-access-token-request
                 code client-id client-secret auth-code-exchange)
        access-token (integrations-helper/http-request request)]
    (swap! jira-integration #(update-in % [:instances integration-name :access-token]
                                        (constantly access-token)))))

(defn set-jira-cloud-id!
  [integration-name]
  (let [integration-map @jira-integration
        instance (get-in integration-map [:instances integration-name])
        access-token (:access-token instance)
        request-deps (:get-cloud-id-request integration-map)
        request (jira-integration/get-cloud-id-request access-token request-deps)
        cloud-id (integrations-helper/http-request request)]
    (swap! jira-integration #(update-in % [:instances integration-name :cloud-id]
                                        (constantly cloud-id)))))

(defn setup-jira-instance!
  [integration-name]
  (exchange-jira-auth-code! integration-name)
  (set-jira-cloud-id! integration-name))

