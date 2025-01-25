(ns dummett-library.views.admin.user
  (:require
   [dummett-library.subs :as subs]
   [reagent.core :as r]
   [re-frame.core :as rf]))

(defn admin? [auth-token]
  true)

(defn page-get [id]
  (.-value (. js/document (getElementById id))))

(defn dropdown [id & menu-items]
  [:div.select
   (reduce
    (fn [acc item]
      (conj
       acc
       [:option {:id (str "dropdown" item)} item]))
    [:select {:id id}]
    menu-items)])

(defn add-user []
  [:form.box
   [:h2.title {:class "is-3"} "Add User"]
   [:div.block
    [:label {:class "label"} "Email"]
    [:div.control {:class "has-icons-left"}
     [:input.input {:type "text" :id "email"}]
     [:span.icon {:class "is-small is-left"}
      [:i {:class "fa fa-envelope" :aria-hidden true}]]]]
   [:div.block
    [:label.label "Password"]
    [:div.control {:class "has-icons-left"}
     [:input.input {:type "text" :id "password"}]
     [:span.icon {:class "is-small is-left"}
      [:i {:class "fa fa-lock" :aria-hidden true}]]]]
   [:div.block
    [:label.label "Role"]
    [dropdown "role-dropdown" "User" "Admin"]
    [:button.button
     {:class "is-success fa fa-plus"
      :style {:float "right"}
      :on-click
      (fn []
        (let [select (. js/document (getElementById "role-dropdown"))
              options (.-options select)
              idx (.-selectedIndex select)
              role (.-value (aget options idx))]
          ;; todo: backend call
          (println (str (page-get "email") " " (page-get "password")
                        " " role))))}]]])

(defn update-user []
  [:div.box
   [:h2.title {:class "is-3"} "Edit User"]])

(defn page []
  [:div.app
   [:section.hero
    [:div.hero-body
     [:p.title  "Manage Users"]]]
    (let [auth-token @(rf/subscribe [::subs/token])]
      (if (and auth-token (admin? auth-token))
        [:div
         [add-user]
         [update-user]]
        [:div
         [:h3.title {:class "is-3"} "Unauthorized"]]))])
