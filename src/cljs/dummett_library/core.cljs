(ns dummett-library.core
  (:require
   [ajax.core :as ajax]
   [dummett-library.views.library :as views]
   [dummett-library.views.admin.add :as views.admin.add]
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
    ["/admin/add" {:name :admin.add
             :view #'views.admin.add/page
             :controllers
             [{:start (fn [_]
                        (rf/dispatch [::events.admin/init-add-page]))}]}]
    ["/login" {:name :login
               :view #'views.login/page
               :controllers
               [{:start (fn [_]
                          (rf/dispatch [::events.login/init-login-page]))}]}]
    ]))

;;   #_[["/" {:name        :prover
;;          :view        #'views/proof-section
;;          :controllers [{:start (fn [_] (rf/dispatch [::events/init-proof-section]))}]}]
;;    ["/tutorial" {:name :tutorial
;;                  :view #'views/tutorial-page
;;                  :controllers
;;                  [{:start (fn [_]
;;                             (rf/dispatch
;;                              [::events/init-tutorial-section]))}]}]
;;    ["/formulas" {:name :formulas
;;                  :view #'views/formulas-page
;;                  :controllers
;;                  [{:start (fn [_]
;;                             (rf/dispatch
;;                              [::events/init-formulas-section]))}]}]]))

;; define your app data so that it doesn't get over-written on reload

;;(defn quote []
;;  (let [data (atom "quote app")]
;;    (fn []
;;      [:div.cards>div.card
;;       [:h2.card-header.text-center "Dummett Library"]
;;       [:div.card-body.text-center
;;        [:p#quote @data]
;;        [:p#author @data]]
;;       [:div.card-footer.center.text-center
;;        [:button#twitter.outline>a#tweet
;;         {:href "#"
;;          :target "_blank"}
;;         [:i.fi-social-twitter " Tweet"]]
;;        [:button#new-quote.outline
;;         [:i.fi-shuffle " New Quote"]]]])))

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
  (start))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))

(main)
