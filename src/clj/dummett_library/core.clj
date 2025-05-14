(ns dummett-library.core
  (:require
   [cheshire.core :as json]
   [clojure.string :as string]
   [compojure.core :as compojure]
   [compojure.route :as route]
   [dummett-library.admin.add :as add]
   [dummett-library.admin.core :as admin]
   [dummett-library.admin.token :as token]
   [dummett-library.admin.user :as user]
   [dummett-library.http :as http]
   [dummett-library.log :as log :refer [defn-logged]]
   [dummett-library.query.core :as query]
   [dummett-library.store.core :as store]
   [org.httpkit.server :as server]
   [ring.middleware.cors :as cors]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.defaults :as middleware])
  (:gen-class))

(defn init! []
  (log/init :info)
  (log/info {:event "initialization" :status "starting"})
  (store/init!)
  (query/init!)
  (admin/init!)
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
  (let [doc-types (if (seq document-types)
                    document-types
                    (query/all-items "document-type"))]
    (map #(assoc % :query-string query-string)
         (query/query doc-types query-string
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

(def req (atom nil))

;; START HERE:
;; ok here's the deal - if I had read a little about ring to start, we could have avoided a lot
;; of the work here. We don't need a document-store in ig. that's done by ring
;; it'll make a temp file when it gets the request, then we just have to process
;; that along with the page stuff.


;;(add/new! my-data (get my-data :archivist))
;; (json/parse-string (get @data :numbering) keyword)
;; => ({:slice-number "", :page-number "294", :style "1"})

(defn document-add-internal
  "The primary interface for adding documents."
  ;;  {:log-level :info :result-fn identity}
  [{{:strs [authorization]} :headers :as request}]
  (reset! req request)
  (let [{token :body} (token/parse authorization)
        token-body (get (json/parse-string token keyword) :body)
        role (get token-body :role)
        archivist (get token-body :email)]
    (if (user/admin? role)
      (add/new! (get request :params) archivist)
      (http/not-authorized))))

(defn add-document! [req]
  {:status 200
   :body  (json/generate-string (document-add-internal req))})

;;; User Management
(defn-logged add-user!
  "Add a user to the application."
  {:log-level :info :result-fn count}
  [{{:strs [authorization]} :headers body :body}]
  ;; status?
  (let [{token-body :body} (token/parse authorization)
        role (get-in (json/parse-string token-body) ["body" "role"])]
    (if (user/admin? role)
      (let [parsed-body (-> body
                            (slurp :encoding "UTF-8")
                            (json/parse-string keyword))
            {:keys [email password role]} parsed-body]
        (user/add! email password role))
      (http/not-authorized))))

(defn-logged remove-user!
  "Remove user from the database."
  {:log-level :info :result-fn identity}
  [{{:strs [authorization]} :headers
    {:strs [email]} :query-params}]
  ;; status?
  (let [{token-body :body} (token/parse authorization)
        {user-email "email" user-role "role"} (get (json/parse-string token-body) "body")]
    (if (or (user/admin? user-role)
            (= email user-email))
      (user/remove! email)
      (http/not-authorized))))

(defn-logged update-role!
  "Update a users role"
  {:log-level :info :result-fn identity}
  [{{:strs [authorization]} :headers body :body}]
  ;; status?
  (let [{token-body :body} (token/parse authorization)
        role (get-in (json/parse-string token-body) ["body" "role"])]
    (if (user/admin? role)
      (let [parsed-body (-> body
                            (slurp :encoding "UTF-8")
                            (json/parse-string keyword))
            {:keys [email role]} parsed-body]
        (user/edit! email :role role))
      (http/not-authorized))))

(defn-logged list-users
  "List all users"
  {:log-level :info :result-fn count}
  [{{:strs [authorization]} :headers}]
  (let [{token-body :body} (token/parse authorization)]
    (if (user/admin? (get-in (json/parse-string token-body) ["body" "role"]))
      (user/list-all)
      (http/not-authorized))))

(defn-logged login
  "Login a user by generating a token."
  {:log-level :info :result-fn identity}
  [{{:strs [authorization]} :headers}]
  (let [{response-body :body status :status} (token/parse authorization)]
    (if (<= 200 status 300)
      (let [{{:keys [email password]} :body} (json/parse-string response-body keyword)]
        (token/token email password))
      (http/not-authorized))))

(compojure/defroutes app
  (compojure/GET "/health-check" [] health-check)
  (compojure/GET "/query" req (query-wrapper req))
  (compojure/POST "/admin/document/add" req (add-document! req))
  (compojure/POST "/admin/user/add" req (add-user! req))
  (compojure/GET "/admin/user/list" req (list-users req))
  (compojure/DELETE "/admin/user/remove" req (remove-user! req))
  (compojure/POST "/admin/user/update" req (update-role! req))
  (compojure/POST "/login" req (login req))
  (route/not-found "<h1>Page not found</h1>"))

(defonce server (atom nil))

(defn start! [& args]
  (let [port 4000]
    (init!)
    (reset!
     server
     (server/run-server
      (cors/wrap-cors
       (wrap-multipart-params (wrap-params (middleware/wrap-defaults #'app middleware/api-defaults)))
       :access-control-allow-origin [#".*"]
       :access-control-allow-methods [:get :put :post :delete]
       :access-control-allow-headers
       #{"accept"
         "accept-encoding"
         "accept-language"
         "authorization"
         "content-type"
         "origin"
         "x-requested-with"})
      {:port port}))))

(defn stop! []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil))
  (store/halt!)
  (query/halt!))

(defn -main [& args]
  (start!))

(comment
  (-main)
  )
