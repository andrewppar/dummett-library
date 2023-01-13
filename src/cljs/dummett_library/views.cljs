(ns dummett-library.views
  (:require-macros [adzerk.env :as env])
  (:require
   [clojure.string :as string]
   [dummett-library.events :as events]
   [dummett-library.subs   :as subs]
   [re-frame.core          :as rf]
   [reagent.core           :as r]))

;; This controls if the navbar is used in concert with other
;; applications. For most purposes it should be set to true.
;; In rare cases it can be configured to link to other applications
;; e.g. for personal use in a wider web context.
(env/def STANDALONE "true")


(defn navbar
  "This specifies a navigation bar.

  The default is to use bulma's navigation bar and hamburger menu"
  []
  (r/with-let [expanded? (r/atom false)]
    [:nav.navbar.is-inverted
     {:class "navbar is-inverted is-fixed-top"}
     [:div.container
      [:div.navbar-brand
       [:a.navbar-item
        {:class "is-size-2"
         :href "/dummett"
         :style {:font-weight :bold}} "𝜑"]
       (when (= STANDALONE "false")
         [:span.navbar-burger.burger
          {:data-target :nav-menu
           :on-click #(swap! expanded? not)
           :class (when @expanded? :is-active)}
          [:span][:span][:span]])]
      [:div.navbar-start
       [:div.navbar-item
        {:class "has-text-centered has-text-white is-size-2"
         :style {:text-transform :capitalize}}
        "Dummett Library"]]
      [:div#nav-menu.navbar-menu
       {:class (when @expanded? :is-active)}
       [:div.navbar-end
        (when (= STANDALONE "false")
          [:a.navbar-item
           {:class "has-text-centered has-text-white"
            :href "/"
            :style {:text-transform :lowercase}}
           "λogos"])]]]]))

(defn show-search-result
  "Show more context for the search result. This opens a dropdown
  that displays the search terms highglighted in some portions
  of the text that was returned."
  [result]
  (let [fragment (first (get result :fragments))]
    (r/with-let [hide?   (r/atom true)]
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
             {:dangerouslySetInnerHTML
              {:__html fragment}}]]]]]]])))

(defn show-all-results [search-results]
  (map
   (fn [result]
     [:a.panel-block
      [:table {:class "table is-fullwidth"}
       [:tr
        [:td (get result :type)]
        [:td [:b (get result :title)]]]
       [show-search-result result]
       ]])
   search-results))

(defn show-focal-result
  "Open a modal to show the full text of a particular result."
  [focal-result]
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

;; An atom to indicate whether or not a search has been run before.
;; This controls whether or not the user gets feedback that their
;; search returned no results.

;; TODO: There may be a better way to do this.
(def search-ran? (r/atom false))

(defn search-result-section
  "Display the results of a search."
  [search-results]
  (let [focal-result @(rf/subscribe [::subs/focal-result])]
    [:section
     [:div {:class "hero-body"}
      [:div {:id "other-text"}
       (if-not (seq search-results)
         (if @search-ran? "No results found" "")
         [:article {:class "panel is-primary"}
          [:p.panel-heading "Search Results"]
          (if (nil? focal-result)
            (show-all-results search-results)
            (show-focal-result focal-result))])]]]))

(defn search
  "Run a search"
  [search-string]
  (reset! search-ran? true)
  (rf/dispatch [::events/search search-string]))

(defn clear-search-results
  "Clear the results of a search"
  []
  (rf/dispatch [::events/clear-search-results]))

(defn start-page
  "This specifies the landing page for the application.

  It has a navbar at the top which only includes a link to this page.

  Includes a search bar with some buttons for running and clearing
  searches.

  Additionally it has space that the search results can be stored once
  a search is run."
  []
  (let [search-results @(rf/subscribe [::subs/search-results])]
    [:div.app
     [:div {:class "has-navbar-fixed-top"}
      [:div.section
      [navbar]]
      [:div.section {:class "is-small"}
       [:div {:class "hero-body"}
        [:div {:class "field has-addons"}
         [:div.control
          [:a {:class "button is-danger"
               :on-click #(clear-search-results)}
           "Clear"]]
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
      [search-result-section search-results]]]))
