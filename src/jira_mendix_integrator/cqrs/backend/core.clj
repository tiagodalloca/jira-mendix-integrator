(ns jira-mendix-integrator.cqrs.backend.core
  (:require [clojure.core.async :as async]
            [jira-mendix-integrator.cqrs.backend.async-utils :refer
             [create-thread-pool execute-async]]))

(defn- handle-dispach-async [{:keys [observers
                                     handlers
                                     pool]}
                             {:keys [action-t
                                     args
                                     handler-promise
                                     enforce-handler]}]
  (let [action-obs (get @observers action-t)
        action-handler (get @handlers action-t)
        vargs (vector args)
        exceptions-wrapper (fn [f ex-handler]
                             (fn [& args]
                               (try
                                 (apply f args)
                                 (catch Exception e
                                   (ex-handler e)))))]
    (if action-handler
      (execute-async
       pool
       (exceptions-wrapper action-handler (fn [ex] (deliver handler-promise ex)))
       (conj vargs handler-promise))
      (deliver handler-promise
               (if enforce-handler
                 (ex-info
                  (str "No handler found for action " action-t)
                  {:cause ::no-handler-found
                   :action-t action-t
                   :args args})
                 nil)))
    (when action-obs
      (doseq [[_ f] action-obs]
        (execute-async
         pool
         (exceptions-wrapper f identity)
         vargs)))))

(defn start-listening [{:keys [chan exit-chan running?] :as dispatcher}]
  (async/go-loop []
    (async/alt!
      chan ([action-map]
            (handle-dispach-async dispatcher action-map)
            (recur))
      exit-chan (reset! running? false)))
  (reset! (:running? dispatcher) true))

(defn create-dispatcher
  ([{:keys [pool pool-size chan-buf-size chan-buf immediately-start?]}]
   (let [pool-size (or pool-size 8)
         pool (or pool (create-thread-pool pool-size))
         chan (async/chan (or chan-buf chan-buf-size 8))
         dispatcher {:observers (atom {})
                     :handlers (atom {})
                     :chan chan
                     :exit-chan (async/chan)
                     :pool pool
                     :running? (atom false)}]
     (when immediately-start?
       (start-listening dispatcher))
     dispatcher))
  ([] (create-dispatcher {})))

(defn stop-listening [{:keys [exit-chan]}]
  (async/put! exit-chan true))

(defn add-observer [dispatcher action-t observer-id observer]
  (letfn [(add-observer-to-action-t [m]
            (if m
              (assoc m observer-id observer)
              (array-map observer-id observer)))
          (add-observer-to-observers [observers]
            (update observers action-t add-observer-to-action-t))]
    (update dispatcher :observers
            (fn [obs-atom] (swap! obs-atom add-observer-to-observers)))))

(defn remove-observer [dispatcher action-t observer-id]
  (update
   dispatcher :observers
   (fn [obs-atom]
     (swap! obs-atom
            (fn [obs]
              (update obs action-t
                      (fn [m] (when m (dissoc m observer-id)))))))))

(defn add-handler [dispatcher action-t handler]
  (update dispatcher :handlers
          (fn [handlers-atom]
            (swap! handlers-atom
                   (fn [handlers-map]
                     (assoc handlers-map action-t handler))))))

(defn remove-handler [dispatcher action-t]
  (update dispatcher :handlers
          (fn [handlers-atom]
            (swap! handlers-atom
                   (fn [handlers-map]
                     (dissoc handlers-map action-t))))))

(defn dispatch
  ([{:keys [chan]}
    [action-t & args]
    {:keys [enforce-handler]}]
   (let [handler-promise (promise)]
     (async/put! chan {:action-t action-t
                       :args args
                       :handler-promise handler-promise
                       :enforce-handler enforce-handler})
     handler-promise))
  ([dispatcher action]
   (dispatch dispatcher action {})))

(comment
  (let [test-dispatcher (create-dispatcher {:pool-size 1
                                            :chan-buf-size 10
                                            :immediately-start? true})]
    (add-observer test-dispatcher :hi :println-obs println)
    (dispatch test-dispatcher :hi "ola")))

(comment
  (def test-dispatcher (create-dispatcher {:pool-size 10
                                           :chan-buf-size 10
                                           :immediately-start? false}))
  (start-listening test-dispatcher)
  (add-observer test-dispatcher :hi :println-obs println)

  (deref (dispatch test-dispatcher [:hi "hi"] {:enforce-handler true}))
  
  (add-handler
   test-dispatcher :hi
   (fn [[_] handler-promise]
     (println "asdf")
     (Thread/sleep 1000)
     (when handler-promise (deliver handler-promise "hello there!"))))

  (deref (dispatch test-dispatcher [:hi "hi"] {:enforce-handler true}))

  (time
   (dotimes [_ 100]
     (dispatch test-dispatcher :hi "hi")))

  (remove-observer test-dispatcher :hi :println-obs)
  (remove-handler test-dispatcher :hi)
  (stop-listening test-dispatcher)

  test-dispatcher)

