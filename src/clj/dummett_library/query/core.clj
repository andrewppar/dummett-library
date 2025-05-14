(ns dummett-library.query.core
  (:require
   [dummett-library.query.formatter :as formatter]
   [dummett-library.query.query :as query]
   [dummett-library.store.core :as store]
   [dummett-library.store.searcher :as searcher]
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

;;(def all-items (memoize (partial query/all-items (store/store))))

(defn all-items [item-type]
  (query/all-items (store/store) item-type))

(defn query
  "Run a query for any pages matching `query-string`"
  [document-types query-string & {:keys [limit]}]
  (let [hit-count (or limit (hits-per-page))
        formatter (formatter)
        analyzer (store/analyzer)
        searcher (searcher/make (store/store))]
    (if (and hit-count formatter)
      (query/page
       searcher analyzer (store/store) formatter hit-count document-types query-string)
      (throw (ex-info "Query State not initialized" {})))))

(defn record
  "Find the record for `author` or (inclusive) `title`."
  [& {:keys [author title document-type] :as arguments}]
  (when-not (or author title document-type)
    (throw
     (ex-info "Must specify one of author, title, or document-type to find a record."
              {:caused-by arguments})))
  (query/record (searcher/make (store/store)) author title document-type))

(defn user
  "Get user with `email`"
  [email]
  (query/user (searcher/make (store/store)) email))

(defn list-users
  "List all users."
  [analyzer]
  (query/list-users (searcher/make (store/store)) analyzer))


(comment
  @state
  )
