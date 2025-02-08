(ns dummett-library.store.store
  (:import
   (org.apache.lucene.store NIOFSDirectory)
   (java.nio.file Paths)))

(defn make
  "Creates an index store. Takes `index-location` as a keyword arg which
  defaults to `resources/dummett-index`."
  [& {:keys [index-location]
      :or {index-location "/opt/dummett"}}]
  (let [index-path (Paths/get "." (into-array [index-location]))]
    (NIOFSDirectory. index-path)))

(defn close [store]
  (.close store))
