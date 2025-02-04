(ns dummett-library.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 ::search-results
 (fn [db] (get db :search-results)))

(rf/reg-sub
 ::focal-result
 (fn [db] (get db :focal-result)))

(rf/reg-sub
 ::focal-showing?
 (fn [db] (boolean (get db :focal-result))))

(rf/reg-sub
 ::token
 (fn [db] (get db :token)))

(rf/reg-sub
 ::email
 (fn [db] (get db :email)))

(rf/reg-sub
 ::login-error
 (fn [db] (get db :login-error)))

(rf/reg-sub
 ::admin-success
 (fn [db] (get db :admin-success)))

(rf/reg-sub
 ::user-list
 (fn [db] (get db :users)))
