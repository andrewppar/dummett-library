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
     [:p.title "Login"]]]
   [:div.box
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
        [:form.box
         [:div.block
          [:label {:class "label"} "Email"]
          [:div.control {:class "has-icons-left"}
           [:input {:type "text" :id "email" :class "input"}]
           [:span.icon {:class "is-small is-left"}
            [:i {:class "fa fa-envelope" :aria-hidden true}]]]]
         [:div.block
          [:label {:class "label"} "Password"]
          [:div.control {:class "has-icons-left"}
           [:input {:type "password" :id "password" :class "input"}]
           [:span.icon {:class "is-small is-left"}
            [:i {:class "fa fa-lock" :aria-hidden true}]]]]
         [:div
          [:button
           {:type "submit"
            :class "button is-info"
            :on-click
            (fn []
              (let [email (get-value "email")
                    password (get-value "password")]
                (rf/dispatch [::login/set-auth-token email password])))}
           "Submit"]]]))]])
