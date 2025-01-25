(ns dummett-library.events.admin
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 :fetch-admin-add-page
 (fn [_ _]
   {:dispatch [:set-admin-add-page]}))

(rf/reg-event-fx
 ::init-add-page
 (fn [_ _]
   {:dispatch [:fetch-admin-add-page]}))

(rf/reg-event-db
 :set-admin-add-page
 (fn [db _]
   (assoc db :admin-add-page "")))

(rf/reg-event-fx
 :fetch-admin-user-page
 (fn [_ _]
   {:dispatch [:set-admin-user-page]}))

(rf/reg-event-fx
 ::init-user-page
 (fn [_ _]
   {:dispatch [:fetch-admin-user-page]}))

(rf/reg-event-db
 :set-admin-user-page
 (fn [db _]
   (assoc db :admin-user-page "")))
