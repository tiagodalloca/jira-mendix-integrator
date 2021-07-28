(ns user.apis-auth-test
  (:require [clj-http.client :as http-client]
            [clojure.data.json :as json]
            [integrant.core :as ig]
            [integrant.repl :refer [clear go halt init prep reset reset-all]]
            [jira-mendix-integrator.http.handler :as server-handler]
            [jira-mendix-integrator.http.server :as server]
            [paos.service :as paos-service]
            [paos.wsdl :as wsdl]
            ring.util.codec))

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

(defonce jira-code (atom nil))

(defn set-jira-code! [code]
  (reset! jira-code code))

(defn runtime-exec [command-str]
  (-> (Runtime/getRuntime) (.exec command-str)))

(defn get-windows-user []
  (comment
    (runtime-exec "powershell.exe '$env:UserName'"))
  "topc")

(def client-id "hR8vp2JYgGUQBItgZ0HuB5JLXoPzpZWa")
(def client-secret "y6D1X5nViht3FL3QQ4OUwyuNTJJ7LzOmg4tHgK_Ed5gKQYum586jJdAycTgEAgVx")
(def jira-redirect-uri "https://50df9ec8c908.ngrok.io/auth/jira-oauth-redirect")

(defn jira-auth-redirect-uri []
  (str "https://auth.atlassian.com/authorize?audience=api.atlassian.com&client_id=hR8vp2JYgGUQBItgZ0HuB5JLXoPzpZWa&scope=read%3Ajira-user%20read%3Ajira-work&redirect_uri=https%3A%2F%2F50df9ec8c908.ngrok.io%2Fauth%2Fjira-oauth-redirect&state="
       "clj-" (get-windows-user)
       "&response_type=code&prompt=consent"))

(defn open-auth []
  (runtime-exec (str "powershell.exe Start-Process \"" (jira-auth-redirect-uri) "\"")))

(defn exchange-auth-code-for-access-token
  ([code]
   (when code
     (-> (http-client/post
          "https://auth.atlassian.com/oauth/token"
          {:headers {:content-type "application/json"}
           :body (json/write-str
                  {"grant_type" "authorization_code",
                   "client_id" client-id,
                   "client_secret" client-secret,
                   "code" code,
                   "redirect_uri" jira-redirect-uri})})
         (get :body)
         (json/read-str)
         (get "access_token"))))
  ([]
   (exchange-auth-code-for-access-token @jira-code)))

(defonce jira-access-token (atom nil))
(comment
  (reset!
   jira-access-token
   "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Ik16bERNemsxTVRoRlFVRTJRa0ZGT0VGRk9URkJOREJDTVRRek5EZzJSRVpDT1VKRFJrVXdNZyJ9.eyJodHRwczovL2F0bGFzc2lhbi5jb20vb2F1dGhDbGllbnRJZCI6ImhSOHZwMkpZZ0dVUUJJdGdaMEh1QjVKTFhvUHpwWldhIiwiaHR0cHM6Ly9hdGxhc3NpYW4uY29tL2VtYWlsRG9tYWluIjoiZ21haWwuY29tIiwiaHR0cHM6Ly9hdGxhc3NpYW4uY29tL3N5c3RlbUFjY291bnRJZCI6IjYwY2M5Y2YwYzkwY2IyMDA2ODEyZjc1MyIsImh0dHBzOi8vYXRsYXNzaWFuLmNvbS9zeXN0ZW1BY2NvdW50RW1haWxEb21haW4iOiJjb25uZWN0LmF0bGFzc2lhbi5jb20iLCJodHRwczovL2F0bGFzc2lhbi5jb20vdmVyaWZpZWQiOnRydWUsImh0dHBzOi8vYXRsYXNzaWFuLmNvbS9maXJzdFBhcnR5IjpmYWxzZSwiaHR0cHM6Ly9hdGxhc3NpYW4uY29tLzNsbyI6dHJ1ZSwiaXNzIjoiaHR0cHM6Ly9hdGxhc3NpYW4tYWNjb3VudC1wcm9kLnB1czIuYXV0aDAuY29tLyIsInN1YiI6ImF1dGgwfDcwMTIxOjFhNDBhZWRmLWFmMjEtNDE3MS04ZjkxLWNiZjQ0NGQ5MjRjOSIsImF1ZCI6ImFwaS5hdGxhc3NpYW4uY29tIiwiaWF0IjoxNjI0MDQzNTk5LCJleHAiOjE2MjQwNDcxOTksImF6cCI6ImhSOHZwMkpZZ0dVUUJJdGdaMEh1QjVKTFhvUHpwWldhIiwic2NvcGUiOiJyZWFkOmppcmEtd29yayByZWFkOmppcmEtdXNlciJ9.BSAB9x-V1dX1oi1eGW1Z059MB4PKuTxJS1rXIaLmYdOqqMuTvDb5JfRjjyyUZcJGniChDDSJKjJ3qKFDJGV3oWJHjgJPvd6hyFepf17hlB33_CMiqEr9bTQYLAEQuNqXH1vgBPQQM_ZWEgMHHSzf81aLoICXMSR5tJ3wrdQGWgFBlCccNU5jeU59QqFy4RrvjmiUcoKs2x_zgMDa3c97EBo2V4Nx2kh96wriKOwo9uCgEUz14CrGyuT1EtOFQZlQwtxDzIZEg9y_9RbLq8GqlvwQB_4UK4Qqddza5dZMizj-w65kBbuREyqCz85XJkK1YqR1vPrT349waBMDP6Xg7A"))


(defn exchange-access-token!
  []
  (->> (exchange-auth-code-for-access-token)
       (reset! jira-access-token)))

(defn get-cloudid
  ([access-token]
   (-> (http-client/get
        "https://api.atlassian.com/oauth/token/accessible-resources"
        {:headers {"Authorization" (str "Bearer " access-token)
                   :content-type "application/json"}})
       (get :body)
       (json/read-str)
       (nth 0)
       (get "id")))
  ([]
   (get-cloudid @jira-access-token)))

(defonce cloudid (atom nil))

(defn http-get-issue
  [access-token cloudid issue-key]
  (-> (http-client/get
       (str "https://api.atlassian.com/ex/jira/"
            cloudid
            "/rest/api/3/issue/"
            issue-key)
       {:headers {"Authorization" (str "Bearer " access-token)
                  :content-type "application/json"}})
      (get :body)
      (json/read-str)))

(defn http-jql
  [access-token cloudid jql]
  (-> (http-client/get
       (str "https://api.atlassian.com/ex/jira/"
            cloudid
            "/rest/api/3/search/")
       {:headers {"Authorization" (str "Bearer " access-token)
                  :content-type "application/json"}
        :query-params {"jql" jql}})
      (get :body)
      (json/read-str)))

;; MENDIX

(def mendix-api-key "3e84ca42-536b-4cdb-bf7a-15f706263a64")

(def mendix-wsdl "https://docs.mendix.com/apidocs-mxsdk/apidocs/attachments/9535497/19398865.wsdl")

(defonce mendix-soap-service (atom nil))

(defn set-mendix-soap-service!
  []
  (reset! mendix-soap-service (wsdl/parse mendix-wsdl)))

(comment (set-mendix-soap-service!))

(defn mendix-get-sprints
  []
  (let [soap-service @mendix-soap-service
        op (get-in soap-service ["StoriesAPISoap" :operations "GetSprints"])
        url (get-in soap-service ["StoriesAPISoap" :url])
        content-type (paos-service/content-type op)
        headers (paos-service/soap-headers op)
        op-mapping (paos-service/request-mapping op)
        context (-> op-mapping
                    (assoc-in
                     ["Envelope" "Header" "authentication"]
                     {"username" {:__value "PlatformAPIUser"}
                      "password" {:__value "PlatformAPIPassword"}})
                    (assoc-in
                     ["Envelope" "Body" "GetSprints"]
                     {"ProjectID" {:__value "1b474997-f874-41ea-af9e-99acfdfe6935"}
                      "ApiKey" {:__value mendix-api-key}}))
        body (paos-service/wrap-body op context)
        parse-fn (partial paos-service/parse-response op)]
    (try
      (-> url
          (http-client/post
           {:content-type content-type
            :headers headers
            :body body})
          :body          
          parse-fn)
      (catch clojure.lang.ExceptionInfo e
        (->> e ex-data :body (paos-service/parse-fault op))))))

(comment (prn (mendix-get-sprints)))

(defn mendix-create-story
  [{:keys [name description type]} sprint-id]
  (let [soap-service @mendix-soap-service
        op (get-in soap-service ["StoriesAPISoap" :operations "CreateStory"])
        url (get-in soap-service ["StoriesAPISoap" :url])
        content-type (paos-service/content-type op)
        headers (paos-service/soap-headers op)
        op-mapping (paos-service/request-mapping op)
        context (-> op-mapping
                    (assoc-in
                     ["Envelope" "Header" "authentication"]
                     {"username" {:__value "PlatformAPIUser"}
                      "password" {:__value "PlatformAPIPassword"}})
                    (assoc-in
                     ["Envelope" "Body" "CreateStory"]
                     {"Name" {:__value name}
                      "Description" {:__value description}
                      "StoryType" {:__value (condp type identity
                                              :feature "Feature"
                                              :bug "Bug"
                                              "Feature")}
                      "SprintID" {:__value sprint-id}
                      "ProjectID" {:__value "1b474997-f874-41ea-af9e-99acfdfe6935"}
                      "ApiKey" {:__value mendix-api-key}}))
        body (paos-service/wrap-body op context)
        parse-fn (partial paos-service/parse-response op)]
    (try
      (-> url
          (http-client/post
           {:content-type content-type
            :headers headers
            :body body})
          :body          
          parse-fn)
      (catch clojure.lang.ExceptionInfo e
        (->> e ex-data :body (paos-service/parse-fault op))))))

(comment
  (prep)
  (init)
  (clear)
  (reset))

