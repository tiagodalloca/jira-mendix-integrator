(ns jira-mendix-integrator.scheduler.core
  (:require [jira-mendix-integrator.scheduler.protocols :as p]
            [chime.core :as chime]))

(defn periods-of-minutes
  ([mins start]
   (chime/periodic-seq start (java.time.Duration/ofSeconds (* mins 60))))
  ([mins]
   (periods-of-minutes mins (java.time.Instant/now))))

(comment
  (let [formatter (-> (java.time.format.DateTimeFormatter/ofPattern "yyyy/MM/dd HH:mm")
                      (.withZone (java.time.ZoneId/of "UTC-3")))]
    (->> (periods-of-minutes 5)
         (take 5)
         (map #(.format formatter %)))))

(defrecord StatefulScheduler
    [scheduled-jobs]
  p/Scheduler
  (add-job [_ job-id scheduled-job]
    (swap! scheduled-jobs assoc job-id scheduled-job))
  (remove-job [_ job-id]
    (swap! scheduled-jobs dissoc job-id))
  (get-job [_ job-id]
    (-> scheduled-jobs deref (get job-id))))

(defrecord ChimeScheduledJob
    [times f opts chime-scheduled-job]
  p/ScheduledJob
  (start [this]
    (reset! chime-scheduled-job (chime/chime-at times f opts)))
  (stop [this]
    (swap! chime-scheduled-job #(some-> % .close))))

(defn create-scheduler
  []
  (->StatefulScheduler (atom {})))

(defn create-scheduled-job
  ([times f opts]
   (->ChimeScheduledJob times f opts (atom nil)))
  ([times f]
   (create-scheduled-job times f nil)))

(comment
  (def sch (create-scheduler))
  (p/add-job sch
             "test"
             (create-scheduled-job
              (periods-of-minutes 1/6)
              (fn [_] (println "Chiminggg"))))
  (-> sch (p/get-job "test") p/start)
  (-> sch (p/get-job "test"))
  (-> sch (p/get-job "test") p/stop))

(comment
  (let [now (java.time.Instant/now)]
    (chime/chime-at [(.plusSeconds now 2)
                     (.plusSeconds now 4)]

                    (fn [time]
                      (println "Chiming at" time))

                    {:on-finished (fn []
                                    (println "Schedule finished."))})))
