(ns dummett-library.events
  (:require-macros [adzerk.env  :as env])
  (:require
   [ajax.core                   :as ajax]
   [day8.re-frame.http-fx]
   [dummett-library.db          :as db]
   [goog.string                 :as gstring]
   [re-frame.core               :as rf]))


(env/def DUMMETT_SERVER "NOPE")


(rf/reg-event-fx
 ::fetch-search-results
 (fn [_ [_ search-string]]
   (println DUMMETT_SERVER)
   (let [encoded-search (gstring/urlEncode search-string "UTF-8")]
     {:http-xhrio {:uri (str
                         "http://" DUMMETT_SERVER
                         "/query?query-string=" encoded-search)
                   :method :post
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
 (fn [_ _]
   db/default-db))
