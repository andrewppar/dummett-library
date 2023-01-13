(ns dummett-library.parse
  (:require [clojure.java.io :as io]
            [clojure.xml     :as xml])
  (:import
   (org.apache.tika.parser.pdf PDFParser)
   (org.apache.tika.metadata Metadata)
   (org.apache.tika.parser ParseContext)
   (org.apache.tika.sax ToXMLContentHandler)))

(defn pdf->xml-map
  "Given the `filepath` of a pdf document generates a map representing
  an xml parse of that the pdf at `filepath`"
  [filepath]
  (let [parser        (PDFParser.)
        stream        (io/input-stream filepath)
        handler       (ToXMLContentHandler.)
        metadata      (Metadata.)
        parse-context (ParseContext.)]
    (.parse parser stream handler metadata parse-context)
    (-> handler
        .toString
        .getBytes
        io/input-stream
        xml/parse)))

(def ^:private -get
  "Like `get` but symmetrical"
  (fn [k m]
    (get m k)))


(defn xml-page->text
  "Convert an map of an xml representation of a page to text"
  [xml-page]
  (->> xml-page
       (-get :content)
       (reduce
        (fn [result paragraph]
          (if-let [texts (get paragraph :content)]
            (format "%s\n%s" result (apply str texts))
          result))
        "")))

(defn xml-map-pages
  "Given an `xml-map` return the vector of xml representations of pages
  in that `xml-map`."
  [xml-map]
  (->> xml-map
       (-get :content)
       (filter
        (fn [item]
          (= (get item :tag) :body)))
       first
       (-get :content)
       (map xml-page->text)
       (filter (fn [page] (not (= page ""))))))

(comment ;; This isn't really reliable
  (defn xml-map-title
    "Search the metadata of a pdf for the title"
    [xml-map]
    (let [head-content (->> xml-map
                            (-get :content)
                            (filter
                             (fn [item]
                               (= (get item :tag) :head)))
                            first
                            (-get :content))]
      (if-let [name-title (filter
                           (fn [item]
                             (= "dc:title"
                                (get (get item :attrs) :name)))
                           head-content)]
        (get (get (first name-title) :attrs) :content)
        (when-let [tag-title (filter
                              (fn [item]
                                (= (get item :tag) :title)))]
          (first (get (first tag-title) :content))))))
  )
