(ns dummett-library.query.formatter
  (:import
   (org.apache.lucene.search.highlight SimpleHTMLFormatter)))

(defn make []
  (SimpleHTMLFormatter.))
