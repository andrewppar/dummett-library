(ns dummett-library.analyze
  (:import
   (org.apache.lucene.analysis.standard StandardAnalyzer)))

(defn new-analyzer []
  (StandardAnalyzer.))


