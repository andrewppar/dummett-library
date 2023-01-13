(ns dummett-library.subs
  (:require [re-frame.core :as rf]))

;; A subscription to the search results so that if the database changes
;; any necessary views can be updated.
(rf/reg-sub
 ::search-results
 (fn [db]
   (get db :search-results)))

;; A subscription to the focal results.
(rf/reg-sub
 ::focal-result
 (fn [db]
   (get db :focal-result)))

;; A subscription to indicate whether or not the focal result is showing
;; this is used to display the modal that contains the full page in the
;; focal result.
(rf/reg-sub
 ::focal-showing?
 (fn [db]
   (boolean (get db :focal-result))))
