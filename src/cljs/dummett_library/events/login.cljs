(ns dummett-library.events.login
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]))

(rf/reg-event-db
 ::set-auth-db-interanal
 (fn [db [_ email token]]
   (assoc db :email email :token token)))

(rf/reg-event-fx
 ::set-auth-token-internal
 (fn [_ [_ response]]
   (println response)))

(rf/reg-event-db
 ::set-auth-error
 (fn [db [_ response]]
   (println response)
   (assoc db :error response)))

(rf/reg-event-fx
 ::set-auth-token
 (fn [_ [_ email password]]
   (let [creds (str email ":" password)
         auth (str "Basic " (js/btoa creds))]
     {:http-xhrio {:uri (str "http://" "localhost:4000" "/login")
                   :method :post
                   :headers {:authorization auth}
                   :format (ajax/transit-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [::set-auth-token-internal]
                   :on-failure [::set-auth-error]}})))

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
