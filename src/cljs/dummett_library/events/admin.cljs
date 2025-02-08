(ns dummett-library.events.admin
  (:require
   [ajax.core :as ajax]
   [dummett-library.events.config :as cfg]
   [dummett-library.events.login :as login]
   [re-frame.core :as rf]))

(rf/reg-event-db
 ::admin-error
 (fn [db error]
   (-> db
       (assoc :admin-error error)
       (dissoc :admin-success))))

(rf/reg-event-db
 ::admin-success-db
 (fn [db [_ message]]
   (-> db
       (assoc :admin-success message)
       (dissoc :admin-error))))

(rf/reg-event-fx
 ::admin-success
 (fn [_ [_ {:keys [message] :as resp}]]
   (rf/dispatch [::admin-success-db message])
   (rf/dispatch [::fetch-users])))

(rf/reg-event-db
 ::remove-admin-success-internal
 (fn [db _]
   (dissoc db :admin-success)))

(rf/reg-event-fx
 ::remove-admin-success
 (fn [_ _]
   {:dispatch [::remove-admin-success-internal]}))

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

(rf/reg-event-fx
 ::admin-failure
 (fn [_ {:keys [status] :as rsp}]
   (when (= status 401)
     (rf/dispatch [::login/remove-auth-token]))
   (rf/dispatch [::admin-error "unauthorized"])))

(rf/reg-event-fx
 ::add-user
 (fn [_ [_ email password role]]
   (if-let [token (. js/sessionStorage getItem "token")]
     {:http-xhrio
      {:uri (str "http://" cfg/library-host "/admin/user/add")
       :method :post
       :headers {:authorization (str "Bearer " token)}
       :params {"email" email "password" password "role"role}
       :format (ajax/json-request-format)
       :response-format (ajax/json-response-format {:keywords? true})
       :on-success [::admin-success]
       :on-failure [::admin-failure]}}
     {:dispatch [::login/remove-auth-token]})))

(rf/reg-event-db
 ::set-user-list
 (fn [db [_ {:keys [status] :as response}]]
   (if (= status 401)
     (rf/dispatch [::login/remove-auth-token])
     (assoc db :users response))))

(rf/reg-event-fx
 ::fetch-users
 (fn [_ [_ _]]
   (if-let [token (. js/sessionStorage getItem "token")]
     {:http-xhrio
      {:uri (str "http://" cfg/library-host "/admin/user/list")
       :method :get
       :headers {:authorization (str "Bearer " token)}
       :format (ajax/transit-request-format)
       :response-format (ajax/json-response-format {:keywords? true})
       :on-success [::set-user-list]
       :on-failure [::set-user-list]}}
     {:dispatch [::login/remove-auth-token]})))

(rf/reg-event-fx
 ::delete-user
 (fn [_ [_ email]]
   (if-let [token (. js/sessionStorage getItem "token")]
     {:http-xhrio
      {:uri (str "http://" cfg/library-host "/admin/user/remove?email=" email)
       :method :delete
       :headers {:authorization (str "Bearer " token)}
       :format (ajax/transit-request-format)
       :response-format (ajax/json-response-format {:keywords? true})
       :on-success [::admin-success]
       :on-failure [::admin-error]}}
     {:dispatch [::login/remove-auth-token]})))

(rf/reg-event-fx
 ::update-role
 (fn [_ [_ email role]]
   (if-let [token (. js/sessionStorage getItem "token")]
     {:http-xhrio
      {:uri (str "http://" cfg/library-host "/admin/user/update")
       :method :post
       :headers {:authorization (str "Bearer " token)}
       :params {"email" email "role" role}
       :format (ajax/json-request-format)
       :response-format (ajax/json-response-format {:keywords? true})
       :on-success [::admin-success]
       :on-failure [::admin-failure]}}
     {:dispatch [::login/remove-auth-token]})))
