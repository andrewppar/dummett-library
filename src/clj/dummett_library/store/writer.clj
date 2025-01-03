(ns dummett-library.store.writer
  (:require
   [dummett-library.store.store :as store])
  (:import
   (org.apache.lucene.index DirectoryReader IndexWriterConfig IndexWriter)))

(defn make
  "Create a new index writer from `analyzer`. Takes an `index-location`
  as a keyword arg which defaults to `/opt/dummett`."
  [analyzer & {:keys [index-location] :or {index-location "/opt/dummett"}}]
  (let [index  (store/make :location index-location)
        config (IndexWriterConfig. analyzer)]
    (IndexWriter. index config)))

(defn open? [writer]
  (.isOpen writer))

(defn commit [writer]
  (.commit writer))

(defn live-commits [writer]
  (.getLiveCommitData writer))

(defn rollback [writer]
  (.rollback writer))

(defn close [writer]
  (.close writer))
