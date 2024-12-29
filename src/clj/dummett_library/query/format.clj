(ns dummett-library.query.format
  (:import
   (org.apache.lucene.search.highlight SimpleHTMLFormatter)))

(defn make []
  (SimpleHTMLFormatter.))
