(ns dummett-library.views.admin.add
  (:require
   [dummett-library.subs :as subs]
   [dummett-library.views.login :as login]
   [dummett-library.views.utils :as utils]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defn get-document-name []
  (when-let [add (. js/document (getElementById "add-document"))]
    (.-name (aget (.-files add) 0))))

(defn upload-document [name]
  [:div.block
   [:div (if @name {:class "file is-info has-name"}  {:class "file is-info has-name"})
    [:label.file-label
     [:input.file-input
      {:type "file" :id "add-document" :name "document"
       :onChange (fn [] (reset! name (get-document-name)))}]
     [:span.file-cta
      [:span.file-icon [:i {:class "fa fa-upload"}]]
      [:span.file-label "New Document"]]
     (if @name
       [:span.file-name @name]
       [:span.file-name "No file uploaded"])]]])

(defn page-input [id page-numbers last?]
  [:div.field {:class "has-addons"}
   [:div.control
    [utils/dropdown
     (str "numbering-dropdown-" id) ["1" "i" "I"]
     :rounded? true :title "numbering style"]]
   [:div.control
    [:input.input {:type "text" :id id :placeholder "range start"}]]
   (if last?
     [:div.control
      [:button.button
       {:class "is-success is-rounded fa fa-plus"
        :style {:float "right"}
        :on-click
        (fn [] (swap! page-numbers conj (count @page-numbers)))}]]
     [:div.control
      [:button.button
       {:class "is-warn is-rounded fa fa-minus"
        :style {:float "right"}
        :on-click
        (fn []
          (swap! page-numbers (comp vec reverse (partial drop 1) reverse)))}]])])

(defn page []
  [:div.app
   [:section.hero
    [:div.hero-body
     [:p.title "Add Documents"]]]
   (if @(rf/subscribe [::subs/token])
     (r/with-let [name (r/atom (get-document-name))]
       (r/with-let [page-numbers (r/atom [0])]
         [:div.box
          (if-not @name
            (upload-document name)
            ;; START HERE: this is where the input for file uploads
            ;; needs to be decided on and formatted
            [:div
             [:div.block [:label.label (str "File: " @name)]]
             [:div.block
              [:label.label "Title"]
              [:input.input
               {:type "text" :id "document-title" :placeholder "Title"}]]
             [:div.block
              [:label.label "Page Numbering"]
              (reduce
               (fn [result id]
                 (let [last? (= id (dec (count @page-numbers)))]
                   (conj result (page-input id page-numbers last?))))
               [:div]
               (sort < @page-numbers))]
             [:div.block
              (utils/button "upload" (fn [] (println "UP")))]])]))
     [:div.section [login/page]])])
