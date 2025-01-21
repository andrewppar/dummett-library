(ns dummett-library.views.login
  (:require
   [re-frame.core :as rf]
   [dummett-library.subs :as subs]
   [dummett-library.events.login :as login]))

(defn get-value [element-id]
  (.-value (. js/document (getElementById element-id))))

(defn page []
  [:div.app
   [:section.hero
    [:div.hero-body
     [:p.title "Login"]
     (let [token @(rf/subscribe [::subs/token])]
       (if token
         (let [email  @(rf/subscribe [::subs/email])]
           [:div.control
            [:p "Logged in with: " email]
            [:button {:type "submit"
                      :class "button is-danger"
                      :on-click
                      (fn []
                        (rf/dispatch [::login/remove-auth-token]))}
             "Log Out"]])
         [:form
          [:label {:class "label"} "Email"]
          [:div.control
           [:input {:type "text" :id "email" :class "input"}]]
          [:label {:class "label"} "Password"
           [:input {:type "password" :id "password" :class "input"}]]
          [:div
           [:button
            {:type "submit"
             :class "button is-info"
             :on-click
             (fn []
               (let [email (get-value "email")
                     password (get-value "password")]
                 (rf/dispatch [::login/set-auth-token email password])))}
            "Submit"]]]))]]])
