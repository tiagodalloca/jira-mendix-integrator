(ns user.syncing-queue
  (:require [jira-mendix-integrator.server :as server]
            [jira-mendix-integrator.server.handler :as server-handler]
            [jira-mendix-integrator.integrations.jira :as jira-integration]
            [jira-mendix-integrator.integrations.helpers :as integrations-helper]
            [jira-mendix-integrator.syncing :refer [execute-command execute-interceptor]]
            [jira-mendix-integrator.syncing.infer
             :refer [infer-entity-sync-command]
             :as syncing-infer]
            [user.syncing-queue.syncing-impl :as syncing-impl]
            [paos.wsdl :as wsdl]
            [paos.service :as paos-service]
            
            [integrant.core :as ig]
            [integrant.repl :refer [clear go halt init prep reset reset-all]])
  (:import [java.util.concurrent ArrayBlockingQueue]))

(comment
  (clojure.tools.namespace.repl/refresh))

(declare syncer-handler)

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
    {:url "https://api.atlassian.com/ex/jira/"}}

   ::mendix-integration
   {:api-key "3e84ca42-536b-4cdb-bf7a-15f706263a64"
    :wsdl "https://docs.mendix.com/apidocs-mxsdk/apidocs/attachments/9535497/19398865.wsdl"
    :soap-service nil}

   ::jira-query-producer {:queue (ig/ref ::syncing-queue)
                          :period 2}})

(integrant.repl/set-prep! (constantly config))>

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
  (if-not @jira-integration
    (let [integration-instances
          (into {} (map #(vector % (jira-integration/integration-instance %))) instances)]
      (reset! jira-integration
              (assoc deps :instances integration-instances)))
    @jira-integration))

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

(defn setup-jira-instances!
  []
  (doseq [[integration-name _] (:instances @jira-integration )]
    (exchange-jira-auth-code! integration-name)
    (set-jira-cloud-id! integration-name)))


(defn jira-query
  [integration-name query]
  (let [integration-map @jira-integration
        instance (get-in integration-map [:instances integration-name])
        request-deps (:api-request integration-map)
        request (jira-integration/search-jql-request query instance request-deps)]
    (integrations-helper/http-request request)))

(defonce mendix-integration (atom nil))

(defmethod ig/init-key ::mendix-integration
  [_ {:keys [wsdl] :as config}]
  (if-not @mendix-integration
    (let [soap-service (wsdl/parse wsdl)
          config (assoc config :soap-service soap-service)]
      (reset! mendix-integration config))
    @mendix-integration))

(comment
  (syncing-infer/infer-entity-sync-command
   {:type :integrations.jira.entities/jira-story,
    :id "10008",
    :key "TES-2",
    :summary "issue 2",
    :description nil,
    :sprint-id 1}
   nil))

(comment
  (let [command  (-> (syncing-infer/infer-entity-sync-command
                      {:type :integrations.jira.entities/story,
                       :id "10008",
                       :key "TES-2",
                       :summary "issue 2",
                       :description nil,
                       :sprint-id 1}
                      nil)
                     (assoc-in [:context :deps] @mendix-integration))
        command-result (:command-result (execute-command ::syncing-impl/impl command))]
    (doto command-result prn)))

(defonce queue (atom nil))

(defn datetime-str [inst]
  (let [formatter
        (-> (java.time.format.DateTimeFormatter/ofPattern "yyyy/MM/dd HH:mm")
            (.withZone (java.time.ZoneId/of "UTC-3")))]
    (.format formatter inst)))

(defn now-minus-minutes [minutes]
  (-> (java.time.Instant/now) (.minusSeconds (* 60 minutes))))

(defn jira-update [integration-name project-name str-datetime]
  (doseq [issue (jira-query
                 integration-name
                 (str "project=" \" project-name \"
                      " AND updated > " \" str-datetime \"))]
    (.put @queue issue)))

(defn syncer-handler
  [issue]
  (try
    (println "iniciando sincronização")
    (let [command  (-> (syncing-infer/infer-entity-sync-command issue nil)
                       (assoc-in [:context :deps] @mendix-integration))
          command-result (:command-result (execute-command ::syncing-impl/impl command))]
      (doto command-result prn)
      (println "finalizando sincronização"))
    (catch Exception e
      (prn e))))

(defonce stop-jira-query-producer (atom nil))

(defmethod ig/init-key ::jira-query-producer
  [_ {:keys [period] queue-instance :queue}]
  (reset! queue queue-instance)
  (let [ms-delay (int (* 1000 60 period))]
    (future
      (try
        (while (not @stop-jira-query-producer)
          (jira-update "clj-repl-dalloca" "TES"
                       (-> (now-minus-minutes period) (datetime-str)))
          (Thread/sleep ms-delay))
        (catch Exception e
          (prn e))))))


