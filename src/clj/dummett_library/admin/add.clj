(ns dummett-library.admin.add
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [dummett-library.log :as log]
   [dummett-library.parse :as parse]
   [dummett-library.query.core :as query]
   [dummett-library.store.core :as store]))

(defn document-spec? [object]
  (and (map? object)
       (every?
        (partial contains? (set (keys object)))
        #{:file :title :type :name :numbering})))

(defn parse-numbering-spec [numbering-spec-string]
  (let [ ;; generate and sort
        specs (->> (json/parse-string numbering-spec-string keyword)
                   (mapv
                    (fn [spec]
                      (-> spec
                          (update :slice-number parse-long)
                          (update :page-number parse-long)
                          (update :style string/trim))))
                   (sort-by :page-number)
                   (into []))]
    ;; validate
    ;; starts with 1
    (when-not (= (get (first specs) :page-number) 1)
      (throw (ex-info "Page numbering does not start with 1"
                      {:caused-by {:spec (first specs)}})))
    ;; styles are all supported
    (run!
     (fn [{:keys [style] :as spec}]
       (when-not (contains? #{"1" "i" "I"} style)
         (throw (ex-info "Numbering Specification does not have a valid style"
                         {:caused-by {:spec spec :style style}}))))
     specs)

    ;; numbering is sequential
    (loop [todo specs]
      (let [current-spec (first todo)
            next-spec (second todo)]
        (when next-spec
          (let [{current-slice :slice-number} current-spec
                {next-page :page-number} next-spec]
            (when-not (and
                       current-slice
                       next-page
                       (= (inc current-slice) next-page))
              (throw (ex-info "Numbering Specification is not total."
                              {:caused-by {:spec current-spec
                                           :followed-by next-spec}}))))
          (recur (rest specs)))))
    ;; return
    specs))

(defn process-spec
  "Prepare the document spec to be ingested"
  [{document-type :type :keys [numbering file] :as frontend-spec}]
  (let [path (get file :tempfile)]
    (-> frontend-spec
        (dissoc :file)
        ;; Hardcoded because for now this is the dummet library
        ;; @todo: generalize
        (assoc :author "Michael Dummett")
        (update :numbering parse-numbering-spec)
        (assoc :path path :document-type document-type))))

(defn store-document-from-path!
  "Add pdf at `path` to the the library.
  `author`, `title`, and `document-type` must also be specified."
  [archivist {:keys [path author title document-type numbering]}]
  (if (seq (query/record :author author
                         :title title
                         :document-type document-type))
    (log/warn {:message
               (format
                "Not adding %s: %s (%s). It already exists."
                document-type title author)
               :event "admin.add document"
               :status "won't do"})
    (let [pages (-> path parse/pdf->xml-map parse/xml-map-pages)]
      {:title title
       :author author
       :archivist archivist
       :pages-added
       (store/add-document!
        pages author title document-type
        :archivist archivist)})))

(defn store-document! [archivist document-spec]
  (store-document-from-path! archivist (process-spec document-spec)))

(defn new!
  "Add documents from document specifications."
  [documents archivist]
  (cond
    (document-spec? documents)
    (store-document! archivist documents)

    (and (coll? documents) (every? document-spec? documents))
    (map (partial store-document! archivist) documents)

    :else
    (throw
     (ex-info
      (string/join " "
                   ["Passed argument"
                    "is neither a document specification or"
                    "collection of document specifications"])
      {:caused-by documents}))))


;;(new! (get @dummett-library.core/req :params) "anparisi")

;;(query/record :document-type "article" :author "Michael Dummett" :title "Fundamentalism")
