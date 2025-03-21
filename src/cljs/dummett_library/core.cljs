(ns dummett-library.core
  (:require
   [ajax.core :as ajax]
   [dummett-library.views.library :as views]
   [dummett-library.views.admin.add :as views.admin.add]
   [dummett-library.views.admin.user :as views.admin.user]
   [dummett-library.views.login :as views.login]
   [dummett-library.events.login :as events.login]
   [dummett-library.events.admin :as events.admin]
   [dummett-library.views.navigation :as nav]
   [dummett-library.events.core :as events]
   [re-frame.core :as rf]
   [reagent.dom :as rdom]
   [reitit.core :as reitit]
   [reitit.frontend.easy :as rfe]))

(defonce router
  (reitit/router
   [["/" {:name :home
          :view #'views/start-page
          :controllers
          [{:start (fn [_]
                     (rf/dispatch [::events/init-start-page]))}]}]
    ["/admin"
     ["/add" {:name :admin.add
              :view #'views.admin.add/page
              :controllers
              [{:start
                (fn [_]
                  (rf/dispatch [::events.admin/init-add-page]))}]}]
     ["/user" {:name :admin.user
               :view #'views.admin.user/page
               :controllers
               [{:start
                 (fn [_]
                   (rf/dispatch [::events.admin/init-user-page]))}]}]]
    ["/login" {:name :login
               :view #'views.login/page
               :controllers
               [{:start
                 (fn [_]
                   (rf/dispatch [::events.login/init-login-page]))}]}]]))

(defn start-router! []
  (rfe/start!
   router
   nav/navigate!
   {}))

(defn local-uri? [{:keys [uri]}]
  (not (re-find #"^\w+?://" uri)))

(defn default-headers [request]
  (if (local-uri? request)
    (update request :headers #(merge {"x-csrf-token" js/csrfToken} %))
    request))

(defn load-interceptors! []
  (let [interceptor (ajax/to-interceptor
                     {:name "default headers"
                      :request default-headers})]
    (swap! ajax/default-interceptors conj interceptor)))

(defn ^:dev/after-load start []
  (rf/clear-subscription-cache!)
  (rdom/render [#'views/page]
               (. js/document (getElementById "app"))))


(defn ^:export main []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (start-router!)
  (load-interceptors!)
  (start)
  (rf/dispatch [::events.login/sync-auth-token]))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))

(main)
