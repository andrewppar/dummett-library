(ns dummett-library.views.navigation
  (:require
   [dummett-library.subs :as subs]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defn admin [burger-active?]
  [:div.navbar-item
        {:class "has-dropdown is-hoverable"}
        [:a.navbar-link "admin panel"]
        [:div.navbar-dropdown
         [:a.navbar-item
          {:href "#/admin/add"
           :on-click (fn [] (reset! burger-active? false))} "Add Documents"]]])

(defn navbar []
  (r/with-let [burger-active? (r/atom false)]
    [:nav.navbar.is-inverted
     {:class "is-fixed-top"}
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "𝜑 "]
      [:a.navbar-burger
       (cond-> {:role "button" :aria-label "menu" :aria-expanded false :data-target "navbar"
                :on-click (fn [] (swap! burger-active? not))}
         @burger-active? (assoc :class "is-active"))
       [:span {:aria-hidden true}]
       [:span {:aria-hidden true}]
       [:span {:aria-hidden true}]]]
     [:div.navbar-menu
      (cond-> {:id "navbar"}
        @burger-active? (assoc :class "is-active"))
      [:div.navbar-start
       [:a.navbar-item {:href "/"
                        :on-click (fn [] (reset! burger-active? false))} "Dummett Library"]]
      [:div.navbar-end
       [:a.navbar-item {:href "#/login"
                        :on-click (fn [] (reset! burger-active? false))} "login"]
       (let [auth-token @(rf/subscribe [::subs/token])]
         (when (and auth-token ;; admin-token
                    )
           [admin burger-active?]))]]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))
