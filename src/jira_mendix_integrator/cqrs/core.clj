(ns jira-mendix-integrator.cqrs.core
  (:require [jira-mendix-integrator.cqrs.backend.core :as backend-core]))

(defn create-dispatcher
  ([opts]
   (backend-core/create-dispatcher
    (assoc opts :immediately-start? true)))
  ([] (create-dispatcher {})))

(defn add-handler
  [dispatcher
   {:keys [action-t
           middleware-actions
           handle]}]
  
  (let [middleware-fs
        (for [a middleware-actions]
          (fn [data]
            (-> (backend-core/dispatch
                 dispatcher
                 [a data]
                 {:enforce-handler true})
                deref)))]
    (backend-core/add-handler
     dispatcher
     action-t
     (fn [[data] handler-promise]
       (->> middleware-fs
            (reduce (fn [data f]
                      (merge data (f data))) data)
            (handle)
            (deliver handler-promise))))))

(defn remove-handler
  [dispatcher action-t]
  (backend-core/remove-handler dispatcher action-t))

(defn dispatch
  [dispatcher action-t data]
  (backend-core/dispatch dispatcher [action-t data] {:enforce-handler true}))

(comment
  (def dispatcher (create-dispatcher))

  (add-handler
   dispatcher
   {:action-t :+-2
    :handle #(->> % :+-2/a (+ 2) (hash-map :+-2/v))})

  (-> dispatcher (dispatch :+-2 {:+-2/a 1}) deref)

  (add-handler
   dispatcher
   {:action-t :*-3
    :handle #(->> % :*-3/a (* 3) (hash-map :*-3/v))})

  (-> dispatcher (dispatch :*-3 {:*-3/a 2}) deref)

  (add-handler
   dispatcher
   {:action-t :calc
    :middleware-actions [:+-2 :*-3]
    :handle #(+ (:+-2/v %)
                (:*-3/v %))})

  (-> dispatcher (dispatch :calc {:+-2/a 1 :*-3/a 2}) deref)
  )

