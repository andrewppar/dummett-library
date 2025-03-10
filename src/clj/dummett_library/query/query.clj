(ns dummett-library.query.query
  (:require
   [clojure.string :as str])
  (:import
   (org.apache.lucene.analysis.standard StandardAnalyzer)
   (org.apache.lucene.index Term DirectoryReader)
   (org.apache.lucene.queryparser.classic QueryParser)
   (org.apache.lucene.search
    IndexSearcher BooleanClause$Occur
    BooleanQuery$Builder ScoreDoc TopDocs TermQuery)
   (org.apache.lucene.search.highlight
    Highlighter QueryScorer SimpleSpanFragmenter TokenSources)
   (org.apache.lucene.store NIOFSDirectory)))

(defn ^:private build-doc-types-clause
  [document-types]
  (let [builder (BooleanQuery$Builder.)]
    (reduce
     (fn [_ document-type]
       (let [term (->> document-type (Term. "type") TermQuery.)]
         (.add builder term BooleanClause$Occur/SHOULD)))
     nil document-types)
    (.build builder)))

(defn user-query [email]
  (TermQuery. (Term. "email" (format "%s" email))))

(defn new-query
  "Run a query for pages matching text"
  [analyzer query document-types]
  (let [builder (BooleanQuery$Builder.)
        term    (-> "text"
                    (QueryParser. analyzer)
                    (.parse query))]
    (.add builder term BooleanClause$Occur/MUST)
    (when (seq document-types)
      (let [doc-types-clause (build-doc-types-clause document-types)]
        (.add builder doc-types-clause BooleanClause$Occur/MUST)))
    (.build builder)))

(defn ^:private ->fragments
  [doc doc-id highlighter analyzer store]
  (let [text   (.get doc "text")
        stream (TokenSources/getAnyTokenStream
                (DirectoryReader/open store) doc-id "text" analyzer)
        frags  (.getBestFragments highlighter stream text 100)]
    (map
     (fn [fragment]
       (format "...%s..." (str/replace (str fragment) #"\n" "</br>")))
     frags)))

(defn ^:private ->document
  [^IndexSearcher searcher
   ^ScoreDoc score-doc]
  (let [doc-id (.doc score-doc)
        doc (.doc searcher doc-id)
        fields (.getFields doc)
        ;; Convert a document to a map
        ;; whose keys are field names
        ;; and whose values are vectors
        ;; of the values associated with that key.
        as-map (reduce (fn [acc
                           ^org.apache.lucene.document.Field field]
                         (let [field-key  (keyword (.name field))
                               new-value  (if-let [number (.numericValue field)]
                                            number
                                            (.stringValue field))]
                           (if-let [current-value (get acc field-key)]
                             (assoc acc field-key (conj current-value new-value))
                             (assoc acc field-key [new-value]))))
                       {:doc-id doc-id
                        :lucene-score (.score score-doc)} fields)]
    ;; If as-map has a vector with only one value
    ;; make that the sole value for key
    ;; TODO: Think about Maybe just always use vectors
    ;; and don't do this reduction...
    ;; Maybe we need to track labels we expect
    ;; to have multiple values...
    (reduce-kv (fn [result key value]
                 (if (and (vector? value)
                          (= (count (set value)) 1))

                   (assoc result key (first value))
                   (assoc result key value)))
               {} as-map)))

(defn ^:private ->text-document
  "Convert a Lucene ScoreDoc object
  to a map"
  [^IndexSearcher searcher
   ^StandardAnalyzer analyzer
   ^Highlighter highlighter
   ^NIOFSDirectory store
   ^ScoreDoc score-doc]
  (let [doc-id (.doc score-doc)
        doc (.doc searcher doc-id)]
    (assoc (->document searcher score-doc)
           :fragments (->fragments
                       doc doc-id highlighter analyzer store))))

(defn score-text-search-results
  "Rank search results"
  [^TopDocs search-results
   ^IndexSearcher searcher
   ^StandardAnalyzer analyzer
   ^Highlighter highlighter
   ^NIOFSDirectory store]
  (map
   (fn [doc-idx]
     (let [score-doc (nth (.scoreDocs search-results) doc-idx)]
       (->text-document searcher analyzer highlighter store score-doc)))
   (range (count (.scoreDocs search-results)))))

(defn serialize-user-search-results
  "Show user results"
  [^TopDocs search-results
   ^IndexSearcher searcher]
  (let [docs (.scoreDocs search-results)]
    (map
     (comp (partial ->document searcher) (partial nth docs))
     (range (count docs)))))

(defn query
  "Run a query against the index."
  [searcher analyzer store formatter hits-per-page document-types query-string]
  (let [query (new-query analyzer query-string document-types)
        scorer (QueryScorer. query)
        highlighter (Highlighter. formatter scorer)
        ;; I hardcoded the fragmentation size - I messed with it some
        ;; I don't think we'll really need to worry about changing it.
        fragmenter (SimpleSpanFragmenter. scorer 10)]
    (.setTextFragmenter highlighter fragmenter)
    (-> searcher
        (.search query hits-per-page)
        (score-text-search-results searcher analyzer highlighter store))))

(defn user
  "Query for a user"
  [searcher email]
  (let [query (user-query email)]
    (-> searcher
        (.search query 2)
        (serialize-user-search-results searcher))))

(defn list-users
  "List all users."
  [searcher analyzer]
  (let [user (.parse (QueryParser. "role" analyzer) "user")
        admin (.parse (QueryParser. "role" analyzer) "admin")
        inner-builder  (BooleanQuery$Builder.)]
    (.add inner-builder user BooleanClause$Occur/SHOULD)
    (.add inner-builder admin BooleanClause$Occur/SHOULD)
    (let [inner-query (.build inner-builder)
          builder (BooleanQuery$Builder.)]
      (.add builder inner-query BooleanClause$Occur/MUST)
      (let [query (.build builder)]
        (-> searcher
            (.search query 100)
            (serialize-user-search-results searcher))))))


(defn all-items
  [store field]
  (let [reader  (DirectoryReader/open store)
        indexes (range (.numDocs reader))]
    (->> indexes
         (map (fn [idx] (-> reader (.document idx) (.get field))))
         (filter (complement nil?))
         distinct)))
