(ns dummett-library.views.navigation
  (:require [re-frame.core :as rf]))

(defn navbar []
  [:nav.navbar.is-inverted
   {:class "is-fixed-top"}
   [:div.navbar-brand
    [:a.navbar-item  "𝜑 "]]
   [:div.navbar-menu {:id "navbar"}
    [:div.navbar-start
     [:a.navbar-item {:href "/"} "Dummett Library"]]
    [:div.navbar-end
     [:a.navbar-item {:href "/admin"} "admin"]]]])

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

   ;;[:div.container
    #_[:div.navbar-brand [:a.navbar-item
                        {:class "is-size-2"
                         :href "/"
                         :style {:font-weight :bold}} "𝜑 "]]
;;    [:div.navbar-start
;;     #_[:nav
;;      [:p {:class "has-text-centered has-text-white is-size-2"}
;;       "Dummett Library"]]
;;     [:div.narbar-item {:class "has-text-white has-text-centered is-site-2"}
;;      [:p {:class "level-item"} "admin"]]]
;;  ;;  ]
;;   ])
