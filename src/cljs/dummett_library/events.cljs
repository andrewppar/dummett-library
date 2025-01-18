(ns dummett-library.events
  (:require
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [dummett-library.db :as db]
   [goog.string :as gs]
   [re-frame.core :as rf]
   [reitit.frontend.controllers :as rfc]))

(rf/reg-event-db
  :common/navigate
  (fn [db [_ match]]
    (println db)
    (let [old-match (:common/route db)
          new-match (assoc match :controllers
                           (rfc/apply-controllers
                            (:controllers old-match) match))]
      (assoc db :common/route new-match))))

(rf/reg-event-fx
 ::fetch-search-results
 (fn [_ [_ search-string]]
   (let [encoded-search (gs/urlEncode search-string "UTF-8")]
     {:http-xhrio {:uri (str
                         "http://" "localhost:4000"
                         "/query?query-string=" encoded-search)
                   :method :get
                   :format (ajax/transit-request-format)
                   :response-format (ajax/json-response-format
                                     {:keywords? true})
                   :on-success [::set-query-results]
                   :on-failure [::set-error]}})))


(rf/reg-event-fx
 ::search
 (fn [_ [_ search-string]]
   {:dispatch [::fetch-search-results search-string]}))

(rf/reg-event-db
 ::set-error
 (fn [db [_ error]]
   (assoc db :error (get-in error [:response :caused-by]))))

(rf/reg-event-db
 ::set-query-results
 (fn [db [_ search-results]]
   (assoc db :search-results search-results)))

(rf/reg-event-db
 ::clear-search-results-db
 (fn [db _]
   (assoc db
          :search-results nil
          :focal-result nil)))

(rf/reg-event-fx
 ::clear-search-results
 (fn [_]
   {:dispatch [::clear-search-results-db]}))

(rf/reg-event-db
 ::clear-focal-result-db
 (fn [db _]
   (assoc db :focal-result nil)))

(rf/reg-event-fx
 ::clear-focal-result
 (fn [_]
   {:dispatch [::clear-focal-result-db]}))

(rf/reg-event-fx
  ::set-focal-result
  (fn [_ [_ focal-result]]
    {:dispatch [::set-foocal-result-event focal-result]}))


(rf/reg-event-db
 ::set-foocal-result-event
 (fn [db  [_ focal-result]]
   (assoc db :focal-result focal-result)))

(rf/reg-event-db
 ::initialize-db
 (fn [_ _] db/default-db))

(rf/reg-event-fx
 :fetch-start-page
 (fn [_ _]
   {:dispatch [:set-start-page]}))

(rf/reg-event-fx
 ::init-start-page
 (fn [_ _]
   {:dispatch [:fetch-start-page]}))

(rf/reg-event-db
 :set-start-page
 (fn [db _]
   (assoc db :start-page "")))

(rf/reg-event-fx
 :fetch-admin-page
 (fn [_ _]
   {:dispatch [:set-admin-page]}))

(rf/reg-event-fx
 ::init-admin-page
 (fn [_ _]
   {:dispatch [:fetch-admin-page]}))

(rf/reg-event-db
 :set-admin-page
 (fn [db _]
   (assoc db :admin-page "")))
