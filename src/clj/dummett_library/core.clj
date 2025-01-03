(ns dummett-library.core
  (:require
   [cheshire.core :as json]
   [clojure.string :as string]
   [compojure.core :as compojure]
   [compojure.route :as route]
   [dummett-library.add :as add]
   [dummett-library.log :as log :refer [defn-logged]]
   [dummett-library.query.core :as query]
   [dummett-library.store.core :as store]
   [dummett-library.store.searcher :as searcher]
   [org.httpkit.server :as server]
   [ring.middleware.cors :as cors]
   [ring.middleware.defaults :as middleware])
  (:gen-class))

(defn init! []
  (log/init :info)
  (log/info {:event "initialization" :status "starting"})
  (store/init!)
  (query/init!)
  (log/info {:event "initialization" :status "done"}))

(defn-logged query
  "The primary interface for getting documents.

   The top documents matching `query-string` are retrieved.
   Optionally the keyword `:document-types` can be specified. The
   default value for this is [], which returns all document types.

   For a list of all document types available in the index call
   `(all-document-types)`."
  {:log-level :info :result-fn count}
  [query-string &
   {:keys [document-types hits-per-page] :or {document-types []}}]
  (let [analyzer (store/analyzer)
        store (store/store)
        searcher (searcher/make store)
        doc-types (if (seq document-types)
                    document-types
                    (query/all-items store "type"))]
    (map #(assoc % :query-string query-string)
         (query/query
          searcher analyzer store doc-types query-string
          :hits-per-page hits-per-page))))

(defn health-check [_]
  {:status 200
   :headers {"Content-Type" "text/json"
             "Access-Control-Allow-Origin" "*"
             "Access-Control-Allow-Headers" "x-requested-with"
             "Access-Control-Allow-Methods" "*"}
   :body (json/generate-string {:status "OK"})})


(defn ^:private query-wrapper-internal
  [req]
  (let [query-string (get-in req [:query-params "query-string"])
        hits-per-page (get-in req [:query-params "hits-per-page"])
        document-types (get-in req [:query-params "document-types"] [])]
    {:status 200
     :body (json/generate-string
            (query query-string
                   :document-types document-types
                   :hits-per-page hits-per-page))}))

(defn query-wrapper
  [req]
  (merge {:headers {"Content-Type" "application/json"}}
         (try (query-wrapper-internal req)
              (catch Exception _error
                {:status 500
                 :body (json/generate-string
                        {:error "Cannot process query."})}))))

(defn parse-document-type [candidate]
  (try
    (case (string/lower-case (str candidate))
      ("book") "book"
      ("article") "article")
    (catch Exception _error
      (throw
       (ex-info (format "Cannot parse %s into document type" candidate)
                {:caused-by candidate})))))

(defn clean-document-specs [document-specs]
  (map
   (fn [{raw-document-type :type :as document-spec}]
     (let [document-type (parse-document-type raw-document-type)]
       (-> document-spec
           (assoc :document-type document-type)
           (dissoc :type))))
   document-specs))

(defn add-internal
  "The primary interface for adding documents."
  ;;  {:log-level :info :result-fn identity}
  [{:keys [body]}]
  (let [document-counts (-> body
                            (slurp :encoding "UTF-8")
                            (json/parse-string keyword)
                            clean-document-specs
                            add/all!)]
    document-counts))

(defn add [req]
  {:status 200
   :body  (json/generate-string (add-internal req))})

(compojure/defroutes app
  (compojure/GET "/" req (str req))
  (compojure/GET "/health-check" [] health-check)
  (compojure/GET "/query" req (query-wrapper req))
  (compojure/POST "/document/add" req (add req))
  (route/not-found "<h1>Page not found</h1>"))

(defonce server (atom nil))

(defn start! [& args]
  (let [port 4000]
    (init!)
    (reset!
     server
     (server/run-server
      (cors/wrap-cors
       (middleware/wrap-defaults
        #'app middleware/api-defaults)
       :access-control-allow-origin [#".*"]
       :access-control-allow-methods [:get :put :post :delete]
       :access-control-allow-headers ["Origin" "X-Requested-With"
                                      "Content-Type" "Accept"])
      {:port port}))))

(defn stop! []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil))
  (store/halt!)
  (query/halt!))



(comment
  (start!)

  )
