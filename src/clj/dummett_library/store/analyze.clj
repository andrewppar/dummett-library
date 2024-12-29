(ns dummett-library.store.analyze
  (:import
   (org.apache.lucene.analysis.standard StandardAnalyzer)))

(defn new-analyzer []
  (StandardAnalyzer.))
