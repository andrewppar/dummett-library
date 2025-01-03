(ns dummett-library.store.core
  (:require
   [dummett-library.store.analyzer :as analyzer]
   [dummett-library.store.document :as document]
   [dummett-library.store.store :as store]
   [integrant.core :as ig]
   [dummett-library.store.writer :as writer]))

(defn ^:private get-env
  "Get the value of `env-var` or supply default."
  ([env-var]
   (get-env env-var nil))
  ([env-var default]
   (or (System/getenv env-var) default)))

(defmethod ig/init-key ::index-location [_ _]
  (get-env "INDEX_LOCATION" "/opt/dummett"))

(defmethod ig/init-key ::analyzer [_ _]
  (analyzer/make))

(defmethod ig/init-key ::store [_ {:keys [index-location]}]
  (store/make :index-location index-location))

(defmethod ig/halt-key! ::store [_ store]
  (store/close store))

(defmethod ig/init-key ::writer [_ {:keys [index-location analyzer]}]
  (writer/make analyzer :index-location index-location))

(defmethod ig/halt-key! ::writer [_ writer]
  (writer/close writer))

(def config
  {::index-location []
   ::analyzer []
   ::store {:index-location (ig/ref ::index-location)}
   ::writer {:analyzer (ig/ref ::analyzer)
             :index-location (ig/ref ::analyzer)}})

(def state (atom nil))

(defn init! []
  (when-not (nil? @state)
    (ig/halt! @state))
  (reset! state (ig/init config)))

(defn halt! []
  (ig/halt! @state)
  (reset! state nil))

(defn index-location []
  (get @state ::index-location))

(defn analyzer []
  (get @state ::analyzer))

(defn store []
  (get @state ::store))

(defn writer []
  (get @state ::writer))

(defmacro with-transaction [writer-var & body]
  `(if-let [~writer-var (writer)]
     (try
       (do
         (do ~@body)
         (writer/commit ~writer-var))
       (catch Error error#
         (writer/rollback ~writer-var)
         (let [new-writer# (ig/init-key ::writer
                                       {:analyzer (analyzer)
                                        :index-location (index-location)})]
           (swap! state assoc ::writer new-writer#))
         (throw (Exception. error#))))
     (throw (ex-info "No index writer initialized." {}))))

(defn add-document!
  ;; add offset for page count, etc...
  [pages author title document-type]
  (with-transaction writer
    (let [page-fn (partial document/page author title document-type)]
      (->> pages
           (map-indexed
            (fn [index page] {:page-number (str (inc index)) :content page}))
           (mapv
            (fn [{:keys [page-number content]}]
              (.addDocument writer (page-fn page-number content))))))))


(comment
  @state
  (init!)
  (halt!)

  (writer/live-commits (get @state ::writer))

  (writer/commit  (get @state ::writer))

  )
