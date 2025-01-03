(ns dummett-library.add
  (:require
   [clojure.string :as string]
   [dummett-library.store.core :as store]
   [dummett-library.parse :as parse]))


(defn document-spec? [object]
  (and (map? object)
       (every?
        (partial contains? (set (keys object)))
        #{:path :author :title :document-type})))

(defn one!
  "Add pdf at `path` to the the library.
  `author`, `title`, and `document-type` must also be specified."
  [{:keys [path author title document-type]}]
  (let [pages (-> path parse/pdf->xml-map parse/xml-map-pages)]
    (store/add-document! pages author title document-type)))

(defn all!
  "Add documents from document specifications."
  [documents]
  (cond
    (document-spec? documents)
    (one! documents)

    (and (coll? documents) (every? document-spec? documents))
    (map one! documents)

    :else
    (throw
     (ex-info
      (string/join " "
                   ["Passed argument"
                    "is neither a document specification or"
                    "collection of document specifications"])
      {:caused-by documents}))))
