(ns dummett-library.state
  (:require
   [aero.core                :as aero]
   [dummett-library.analyze  :as analyze]
   [dummett-library.index    :as index]
   [integrant.core           :as ig])
  (:import 
   (org.apache.lucene.search.highlight SimpleHTMLFormatter)))

(defmethod ig/init-key ::catalog [_ _]
  (aero/read-config "resources/catalog.edn"))

(defmethod ig/init-key ::hits-per-page [_ _] 20)

(defmethod ig/init-key ::index-location [_ _]
  (or (System/getenv "INDEX_LOCATION") "/opt/dummett"))

(defmethod ig/init-key ::analyzer [_ _]
  (analyze/new-analyzer))

(defmethod ig/init-key ::store
  [_ {:keys [index-location]}]
  (index/new-index-store :index-location index-location))

(defmethod ig/init-key ::formatter
  [_ _]
  (SimpleHTMLFormatter.))

(defmethod ig/init-key ::writer
  [_ {:keys [analyzer index-location]}]
  (index/new-index-writer analyzer :index-location index-location))

(defmethod ig/halt-key! ::writer [_ writer]
  (when (.hasUncommittedChanges writer)
    (.commit writer))
  (.close  writer))

(defmethod ig/init-key ::searcher [_ {:keys [store]}]
  (index/new-index-searcher store))

(def run-config
  {::catalog []
   ::hits-per-page []
   ::index-location []
   ::store {:index-location (ig/ref ::index-location)} 
   ::analyzer []
   ::searcher {:store (ig/ref ::store)}
   ::formatter []})

(def reindex-config
  {::catalog []
   ::index-location []
   ::store {:index-location (ig/ref ::index-location)} 
   ::analyzer []
   ::writer {:analyzer (ig/ref ::analyzer)
             :index-location (ig/ref ::index-location)}})

(def state (atom nil))

(defn init! [init-type]
  (let [config (case init-type
                 :reindex reindex-config
                 :run run-config)]
  (when-not (nil? @state)
    (ig/halt! @state))
  (reset! state (ig/init config))))

(defn halt! [& components]
  (if (seq components)
    (ig/halt! @state components)
    (ig/halt! @state))
  (reset! state nil))

