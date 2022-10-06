(ns dummett-library.log
  (:require  
   [cheshire.core    :as cheshire]
   [clojure.edn     :as edn]
   [taoensso.timbre :as timbre]))

(defn ^:private log-output
  [{:keys [level msg_ instant]}]
  (let [event (edn/read-string (force msg_))]
    (cheshire/generate-string {:timestamp instant
                               :level level
                               :message event})))

(defn log [level object]
  (case level
    :trace (timbre/trace object)
    :debug (timbre/debug object)
    :info  (timbre/info object)
    :warn  (timbre/warn object)
    :error (timbre/error object)
    :fatal (timbre/fatal object)))

(defn init
  "Initialize logging"
  [log-level]
  (timbre/merge-config! {:min-level log-level
                         :output-fn log-output}))



(defn get-args [args]
  (reduce (fn [acc arg]
            (cond
              (= arg '&)
              acc
              (symbol? arg)
              (conj acc arg)
              (map? arg)
              (concat acc (get arg :keys))
              :else arg))
          [] args))

(defmacro defn-logged
  [fn-name docstring logging-map args & body]
  (when-not (and
             (string? docstring)
             (map? logging-map))
    (throw
     (ex-info (format "Cannot compile %s macro" fn-name)
              {:caused-by '(not
                            (and (string? ~docstring)
                                 (map? ~logging-map)))})))
  (let [fn-name-string (name fn-name)
        result-fn      (if-let [res-fn (get logging-map :result-fn)]
                         res-fn
                         identity)
        log-level      (get logging-map :log-level)
        arg-map        (reduce (fn [acc arg]
                                 (assoc acc (keyword arg) arg))
                               {} (get-args args))
        start-log      (assoc arg-map
                              :status "starting"
                              :event fn-name-string)]
    `(defn ~fn-name ~docstring ~args
       (do
         (log ~log-level ~start-log)
         (let [result# (do ~@body)]
           (log ~log-level {:status "done"
                        :event ~fn-name-string
                        :result (~result-fn result#)})
           result#)))))
 
