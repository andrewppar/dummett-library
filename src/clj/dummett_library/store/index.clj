(ns dummett-library.store.index
  (:require
   [clojure.string :as str]
   [dummett-library.store.document :as doc]
   [dummett-library.parse :as parse])
  (:import (org.apache.lucene.store NIOFSDirectory)
           (org.apache.lucene.index DirectoryReader IndexWriterConfig IndexWriter)
           (org.apache.lucene.search IndexSearcher)
           java.nio.file.Paths))

(defn new-index-store
  "Creates an index store. Takes `index-location` as a keyword arg which
  defaults to `resources/dummett-index`."
  [& {:keys [index-location]
      :or {index-location "/opt/dummett"}}]
  (let [index-path (Paths/get "." (into-array [index-location]))]
    (NIOFSDirectory. index-path)))

(defn new-index-writer
  "Create a new index writer from `analyzer`. Takes an `index-location`
  as a keyword arg which defaults to `/opt/dummett`."
  [analyzer &
   {:keys [index-location]
    :or {index-location "/opt/dummett"}}]
  (let [index  (new-index-store :location index-location)
        config (IndexWriterConfig. analyzer)]
    (IndexWriter. index config)))

(defn new-index-searcher
  "Create a new lucene index searcher for the passed store."
  [^org.apache.lucene.store.BaseDirectory index-store]
  (IndexSearcher. (DirectoryReader/open index-store)))

(defn index-document!
  "Given a filepath to a pdf document, add each page of that pdf
  to the the libraries lucene index"
  [pdf-filepath index-writer author title document-type]
  (let [pages (-> pdf-filepath parse/pdf->xml-map parse/xml-map-pages)]
    (reduce
     (fn [count page]
       (do
         (->> (doc/new-page
               (str/trim author)
               (str/trim title)
               (str/trim document-type)
               (format "%s" count)
               page)
              (.addDocument index-writer))
         (inc count)))
     1 pages)))

;;(defn index-documents!
;;  [root-path files index-writer fields regex default-doc-type]
;;  (mapv
;;   (fn [file]
;;     (println (format "Indexing file %s..." file))
;;     (let [full-fields (into [] (cons :all fields))
;;           meta-values (first
;;                        (re-seq (regal/regex regex) file))
;;           metadata-map (zipmap full-fields meta-values)
;;           author       (get metadata-map :author)
;;           title        (get metadata-map :title)
;;           doc-type     (str/lower-case
;;                         (if-let [dt (get metadata-map :doc-type)]
;;                          dt
;;                          default-doc-type))
;;           full-path    (str root-path file)]
;;     (index-document! full-path index-writer author title doc-type)))
;;   files))
;;
;;(defn reindex!
;;  [catalog index-writer]
;;  (let [root (get catalog :root)]
;;    (mapv
;;     (fn [topic]
;;       (println (format "Indexing topic %s..." topic))
;;       (let [topic-map     (get catalog topic)
;;             topic-root    (str root (get topic-map :root))]
;;         ;;TODO: Pull this out into its own function
;;         (mapv
;;          (fn [default-doc-type]
;;            (println (format "Indexing document type %s..." default-doc-type))
;;            (let [type-map  (get topic-map default-doc-type)
;;                  type-root (str topic-root  (get type-map :root))
;;                  fields    (get-in type-map [:pattern :matches])
;;                  regex     (get-in type-map [:pattern :regex])
;;                  paths     (get type-map :documents)]
;;              (index-documents!
;;               type-root paths index-writer fields regex (name default-doc-type))))
;;          (remove #(= % :root) (keys topic-map)))))
;;     (remove #(= % :root) (keys catalog)))))
