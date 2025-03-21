(ns dummett-library.views.library
  (:require
   [clojure.string :as string]
   [dummett-library.events.core :as events]
   [dummett-library.subs :as subs]
   [dummett-library.views.navigation :as nav]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defn show-search-result [result]
  (let [fragment (first (get result :fragments))]
    (r/with-let [hide? (r/atom true)]
      [:tr
       [:td (get result :page)
        [:button {:class "button is-info is-inverted is-small"
                  :on-click (fn [] (swap! hide? not))}
         (if @hide? "show" "hide")]]
       [:td
        [:a.panel-block
         {:on-click #(rf/dispatch [::events/set-focal-result result])}
         [:table
          {:class (str "table " (when @hide? "is-hidden"))}
          [:tr
           [:td
            [:div
             {:dangerouslySetInnerHTML {:__html fragment}}]]]]]]])))

(defn show-all-results [search-results]
  (map
   (fn [result]
     [:a.panel-block
      [:table {:class "table is-fullwidth"}
       [:tr
        [:td (get result :type)]
        [:td [:b (get result :title)]]]
       [show-search-result result]]])
   search-results))

(defn show-focal-result [focal-result]
  [:div.modal
   {:class (when @(rf/subscribe [::subs/focal-showing?]) "is-active")}
   [:div.modal-background
    {:on-click #(rf/dispatch [::events/clear-focal-result])}]
   [:div.modal-card
    [:header.modal-card-head
     [:p.modal-card-title [:b (get focal-result :title)]]
     [:button.delete
      {:on-click #(rf/dispatch [::events/clear-focal-result])}]]
    [:section.modal-card-body
     [:div {:dangerouslySetInnerHTML
            {:__html (-> focal-result
                         (get :text)
                         (string/replace #"\n" "</br>"))}}]]
    [:footer.modal-card-foot (get focal-result :page)]]])

(defn search-result-section
  [search-results]
  (let [focal-result @(rf/subscribe [::subs/focal-result])]
    [:section
     [:div {:class "hero-body"}
      [:div {:id "other-text"}
       (if-not (seq search-results)
         ""
         [:article {:class "panel is-primary"}
          [:p.panel-heading "Search Results"]
          (if (nil? focal-result)
            (show-all-results search-results)
            (show-focal-result focal-result))])]]]))

(defn search [search-string]
  (rf/dispatch [::events/search search-string]))

(defn clear-search-results []
  (rf/dispatch [::events/clear-search-results]))

(defn start-page []
  (let [search-results @(rf/subscribe [::subs/search-results])]
    [:div.app
     [:div.section {:class "is-small"}
      [:div
       [:div {:class "field has-addons"}
        [:div.control
         [:a {:class "button is-primary"
              :on-click
              (fn []
                (let [search-text (->> (getElementById "search-text")
                                       (. js/document)
                                       .-value)]
                  (search search-text)))}
          "Search"]]
        [:div {:class "control is-expanded"}
         [:input {:class "input"
                  :type "text"
                  :id "search-text"
                  :on-key-press
                  (fn [e]
                    (when (= 13 (.-charCode e))
                      (let [search-text (->> (getElementById "search-text")
                                             (. js/document)
                                             .-value)]
                        (search search-text))))}]]]]]
     (when search-results
       [:div.box
        [search-result-section search-results]])
     [:div.section {:class "is-small"}
      [:button {:class "button is-danger"
                :on-click #(clear-search-results)}
       "Clear"]]]))

(defn page []
  ;; error handling
  (when-let [page @(rf/subscribe [:common/page])]
    [:div
     {:class "has-navbar-fixed-top"}
     [nav/navbar]
     [:div.section [page]]]))
