(ns jira-mendix-integrator.syncing)

(defmulti syncing-command
  "Receives a implementation keyword, command keyword and data."
  (fn [impl command data]
    (vector impl command)))

(defmethod syncing-command
  :default
  [impl command data]
  (throw (ex-info (str "syncing-command "
                       command
                       " not found for implementation "
                       impl)
                  {:impl impl :command command :data data})))

(defmulti syncing-interceptor
  "Receives a implementation keyword, interceptor keyword should return a map containing:
  :enter
  fn that receives a data map a returns a modified version of it and it's called when entering the interceptor
  :leave
  analogous to :enter"
  (fn [impl interceptor]
    (vector impl interceptor)))

(defmethod syncing-interceptor
  :default
  [impl interceptor]
  (throw (ex-info (str "syncing-interceptor "
                       interceptor
                       " not found for implementation "
                       impl)
                  {:impl impl :interceptor interceptor})))


(defn execute-interceptor
  [impl stage interceptor data]
  (let [interceptor-f (get (syncing-interceptor impl interceptor) stage)]
    (if interceptor-f
      (interceptor-f data)
      data)))

(defn execute-command
  [impl {:keys [command interceptors data]}]
  (let [data-enter (reduce #(execute-interceptor impl :enter %2 %1) data interceptors)
        data-command (syncing-command impl command data-enter)
        data-leave (reduce #(execute-interceptor impl :leave %2 %1) data-command
                           (reverse interceptors))]
    data-leave))

(comment
  (defmethod syncing-interceptor [:test :put-number]
    [_ _]
    {:enter (fn [data] (update data :numbers #(conj % (rand-int 10))))})
  (defmethod syncing-command [:test :sum-numbers]
    [_ _ {:keys [numbers] :as data}]
    (assoc data :sum-numbers (reduce + numbers)))

  (execute-command :test
                   {:command :sum-numbers
                    :interceptors [:put-number]
                    :data {:numbers [1 2 3]}}))

