(ns dummett-library.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 ::search-results
 (fn [db]
   (get db :search-results)))

(rf/reg-sub
 ::focal-result
 (fn [db]
   (get db :focal-result)))

(rf/reg-sub
 ::focal-showing?
 (fn [db]
   (boolean (get db :focal-result))))
