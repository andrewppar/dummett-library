(ns dummett-library.store.analyzer
  (:import
   (org.apache.lucene.analysis.standard StandardAnalyzer)))

(defn make []
  (StandardAnalyzer.))
