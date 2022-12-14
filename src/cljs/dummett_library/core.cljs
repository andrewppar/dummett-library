(ns dummett-library.core
  (:require
   [reagent.dom           :as rdom]
   [re-frame.core         :as rf]
   [ajax.core             :as ajax]
   [dummett-library.views :as views]))

;; define your app data so that it doesn't get over-written on reload

(defn local-uri? [{:keys [uri]}]
  (not (re-find #"^\w+?://" uri)))

(defn default-headers [request]
  (if (local-uri? request)
    (-> request
        (update :headers #(merge {"x-csrf-token" js/csrfToken} %)))
    request))

;; injects transit serialization config into request options

(defn load-interceptors! []
  (swap! ajax/default-interceptors
         conj
         (ajax/to-interceptor {:name "default headers"
                               :request default-headers})))

(defonce app-state (atom {:text "Hello world!"}))

(defn quote []
  (let [data (atom "quote app")]
    (fn []
      [:div.cards>div.card
       [:h2.card-header.text-center "Dummett Library"]
       [:div.card-body.text-center
        [:p#quote @data]
        [:p#author @data]]
       [:div.card-footer.center.text-center
        [:button#twitter.outline>a#tweet
         {:href "#"
          :target "_blank"}
         [:i.fi-social-twitter " Tweet"]]
        [:button#new-quote.outline
         [:i.fi-shuffle " New Quote"]]]])))

(defn ^:dev/after-load start []
  (rf/clear-subscription-cache!)
  (rdom/render [#'views/start-page]
               (. js/document (getElementById "app"))))


(defn ^:export main []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (start))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))

(main)
