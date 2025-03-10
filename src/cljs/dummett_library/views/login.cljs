(ns dummett-library.views.login
  (:require
   [re-frame.core :as rf]
   [dummett-library.subs :as subs]
   [dummett-library.views.utils :as u]
   [dummett-library.events.login :as login]))

(defn get-value [element-id]
  (.-value (. js/document (getElementById element-id))))

(defn submit-login-info []
  (let [email (get-value "email")
        password (get-value "password")]
    (rf/dispatch [::login/set-auth-token email password])))

(defn logout []
  (rf/dispatch [::login/remove-auth-token]))



(defn page []
  [:div.app
   [:section.hero
    [:div.hero-body
     [:p.title "Login"]]]
   [:div.box
    (let [token @(rf/subscribe [::subs/token])]
      (if token
        (let [email @(rf/subscribe [::subs/email])]
          [:div.control
           [:div.block [:p "Logged in with: " email]]
           [:div.block [u/button "Log Out" logout :color :danger]]])
        (let [error @(rf/subscribe [::subs/login-error])]
          [:form.box
           (when error
             [:div.block
              [:div.notification {:class "is-danger"} "Login failed"]])
           [u/input :text "email" "Email"
            :placeholder "email address" :icon "fa fa-envelope"]
           [u/input :password "password" "Password"
            :placeholder "password" :icon "fa fa-lock"]
           [:div [u/button "Submit" submit-login-info]]])))]])
