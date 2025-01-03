(ns dummett-library.store.searcher
  (:import (org.apache.lucene.index DirectoryReader)
           (org.apache.lucene.search IndexSearcher)))

(defn make
  "Create a new lucene index searcher for the passed store."
  [^org.apache.lucene.store.BaseDirectory index-store]
  (IndexSearcher. (DirectoryReader/open index-store)))
