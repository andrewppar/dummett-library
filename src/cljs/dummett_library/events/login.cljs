(ns dummett-library.events.login
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 :set-auth-internal
 (fn [db [_ email token]]
   (assoc db :email email :token token)))

(rf/reg-event-fx
 ::set-auth-token
 (fn [_ [_ email password]]
   (. js/sessionStorage setItem "token" (str email password))
   (. js/sessionStorage setItem "email" (str email))
   {:dispatch [:set-auth-internal email password]}))

(rf/reg-event-db
 :sync-auth-internal
 (fn [db [_ email token]]
   (assoc db :email email :token token)))

(rf/reg-event-fx
 ::sync-auth-token
 (fn [_ [_ _]]
   (let [token (. js/sessionStorage getItem "token")
         email (. js/sessionStorage getItem "email")]
     {:dispatch [:sync-auth-internal email token]})))

(rf/reg-event-fx
 ::remove-auth-token
 (fn [_ [_ _]]
   (. js/sessionStorage removeItem "token")
   (. js/sessionStorage removeItem "email")
   {:dispatch [:remove-auth-internal]}))

(rf/reg-event-db
 :remove-auth-internal
 (fn [db _]
   (dissoc db :email :token)))

(rf/reg-event-fx
 :fetch-login-page
 (fn [_ _]
   {:dispatch [:set-login-page]}))

(rf/reg-event-fx
 ::init-login-page
 (fn [_ _]
   {:dispatch [:fetch-login-page]}))

(rf/reg-event-db
 :set-login-page
 (fn [db _]
   (assoc db :login-page "")))
