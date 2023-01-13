(ns dummett-library.core
  (:require
   [clojure.data.json        :as json]
   [compojure.core           :as compojure]
   [compojure.route          :as route]
   [dummett-library.index    :as index]
   [dummett-library.log      :as log :refer [defn-logged]]
   [dummett-library.query    :as query]
   [dummett-library.state    :as state]
   [org.httpkit.server       :as server]
   [ring.middleware.cors     :as cors]
   [ring.middleware.defaults :as middleware]))

(defn init!
  "Initialize the the dummet library

  There are two types of initialization:
   the first type is for running the application. The second is for
   indexing all of the documents specified in the catalog.edn."
  ([]
   (init! :run))
  ([init-type]
   (do
     (log/init :info)
     (log/log
      :info {:event "initialization"
             :status "starting"})
     (state/init! init-type)
     (log/log
      :info {:event "initialization"
             :status "done"}))))

(defn reindex!
  "Rebuild the library's lucene index from the catalog"
  [& _]
  (init! :reindex)
  (let [catalog (get @state/state ::state/catalog)
        writer  (get @state/state ::state/writer)]
    (index/reindex! catalog writer)
    (.commit writer)
    (.close writer)))

(defn-logged query
  "The primary interface for getting documents.

   The top documents matching `query-string` are retrieved.
   Optionally the keyword `:document-types` can be specified. The
   default value for this is [], which returns all document types.

   For a list of all document types available in the index call
   `(all-document-types)`."
  {:log-level :info :result-fn count}
  [query-string & {:keys [document-types] :or {document-types []}}]
  (let [searcher  (get @state/state ::state/searcher)
        analyzer  (get @state/state ::state/analyzer)
        store     (get @state/state ::state/store)
        doc-types (if (seq document-types)
                    document-types
                    (query/all-items store "type"))]
    (map #(assoc % :query-string query-string)
         (query/query searcher analyzer store query-string doc-types))))

(defn health-check
  "Perform a health check on the dummett library"
  [_]
  {:status 200
   :headers {"Content-Type" "text/json"
             "Access-Control-Allow-Origin" "*"
             "Access-Control-Allow-Headers" "x-requested-with"
             "Access-Control-Allow-Methods" "*"}

   :body (json/write-str {:status "OK"})})


(defn ^:private query-wrapper-internal
  "Middle ware for running a query against the lucene indexes for the
  library."
  [req]
  (let [query-string   (-> req (get :query-params) (get "query-string"))
        document-types (if-let [doc-types (-> req
                                              (get :query-params)
                                              (get "document-types"))]
                         doc-types
                         [])
        result (query query-string :document-types document-types)]
    result))

(defn query-wrapper
  "Run a query against the libraries lucene indexes and return the
  output as a json payload."
  [req]
  {:status 200
   :headers {"Content-Type"  "text/json"
             "Access-Control-Allow-Credentials" "true"
             "Access-Control-Allow-Origin" "*"
             "Access-Control-Allow-Headers" "x-requested-with"
             "Access-Control-Allow-Methods" "*"
             }
   :body (json/write-str (query-wrapper-internal req))})

(compojure/defroutes app
  (compojure/GET "/" req (str req))
  (compojure/GET "/health-check" [] health-check)
  (compojure/POST "/query"        [] query-wrapper)
  (route/not-found "<h1>Page not found</h1>"))

(defn run!
  "Run the application for search."
  [& args]
  (let [port 4000]
    (init!)
    (server/run-server
     (cors/wrap-cors
      (middleware/wrap-defaults
       #'app middleware/api-defaults)
      :access-control-allow-origin [#".*"]
      :access-control-allow-methods [:get :put :post :delete]
      :access-control-allow-headers ["Origin" "X-Requested-With"
                                     "Content-Type" "Accept"])
     {:port port})))
