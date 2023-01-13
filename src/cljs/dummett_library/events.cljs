(ns dummett-library.events
  (:require-macros [adzerk.env  :as env])
  (:require
   [ajax.core                   :as ajax]
   [day8.re-frame.http-fx]
   [dummett-library.db          :as db]
   [goog.string                 :as gstring]
   [re-frame.core               :as rf]))


;; This environment variable tells the frontend where to send it's
;; requests to the backend.
(env/def DUMMETT_SERVER "NO SERVER SET")


;; Given a search string run a get the backend to run a search
;; against the lucene database.
(rf/reg-event-fx
 ::fetch-search-results
 (fn [_ [_ search-string]]
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

;; Run a search by calling ::fetch-search-results with
;; the search string
(rf/reg-event-fx
 ::search
 (fn [_ [_ search-string]]
   {:dispatch [::fetch-search-results search-string]}))

;; Update the database with an error if one is encountered.
(rf/reg-event-db
 ::set-error
 (fn [db [_ error]]
   (assoc db :error (get-in error [:response :caused-by]))))

;; Save the results of a search in the application database
(rf/reg-event-db
 ::set-query-results
 (fn [db [_ search-results]]
   (assoc db :search-results search-results)))


;; Clear the search results from the application database
(rf/reg-event-db
 ::clear-search-results-db
 (fn [db _]
   (assoc db
          :search-results nil
          :focal-result nil)))

;; This is an event (caused by a button push) to
;; clear the search results from the application database.
(rf/reg-event-fx
 ::clear-search-results
 (fn [_]
   {:dispatch [::clear-search-results-db]}))

;; Clear the focal result from the database
;; The focal result shows up as a full page in its own modal.
(rf/reg-event-db
 ::clear-focal-result-db
 (fn [db _]
   (assoc db :focal-result nil)))

;; Used by a button to clear the focal result from the database.
(rf/reg-event-fx
 ::clear-focal-result
 (fn [_]
   {:dispatch [::clear-focal-result-db]}))

;; Given a focal result, set it in the database so it can be used to
;; display a full page of text in a modal.
(rf/reg-event-fx
  ::set-focal-result
  (fn [_ [_ focal-result]]
    {:dispatch [::set-foocal-result-event focal-result]}))

;; The database event for setting a focal result.
(rf/reg-event-db
 ::set-foocal-result-event
 (fn [db  [_ focal-result]]
   (assoc db :focal-result focal-result)))

;; Initialize the application database, this initializes it to {}
(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))
