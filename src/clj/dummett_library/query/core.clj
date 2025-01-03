(ns dummett-library.query.core
  (:require
   [dummett-library.query.formatter :as formatter]
   [dummett-library.query.query :as query]
   [integrant.core :as ig]))

(defmethod ig/init-key ::hits-per-page [_ _]
  (or (System/getenv "HITS_PER_PAGE") 20))

(defmethod ig/init-key ::formatter [_ _]
  (formatter/make))

(def config
  {::hits-per-page []
   ::formatter []})

(def state (atom nil))

(defn init! []
  (when-not (nil? @state)
    (ig/halt! @state))
  (reset! state (ig/init config)))

(defn hits-per-page []
  (get @state ::hits-per-page))

(defn formatter []
  (get @state ::formatter))

(defn halt! []
  (ig/halt! @state)
  (reset! state nil))

(def all-items (memoize query/all-items))

(defn query
  [searcher analyzer store document-types query-string
   & {:keys [hits-per-page]}]
  (let [hit-count (or hits-per-page (hits-per-page))
        formatter (formatter)]
    (if (and hit-count formatter)
      (query/query
       searcher analyzer store formatter hit-count document-types query-string)
      (throw (ex-info "Query State not initialized" {})))))
