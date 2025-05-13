(ns dummett-library.views.admin.add
  (:require
   [clojure.string :as string]
   [dummett-library.events.admin :as admin]
   [dummett-library.subs :as subs]
   [dummett-library.views.login :as login]
   [dummett-library.views.utils :as utils]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defn get-document-name []
  (when-let [add (utils/get-element "add-document")]
    (.-name (aget (.-files add) 0))))

(defn get-document-type []
  (let [select (utils/get-element "document-type")
        idx (.-selectedIndex select)
        options (.-options select)]
    (string/lower-case (.-value (aget options idx)))))

;;(defn get-document-numbering [])

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

(defn upload-one! []
  (when-let [element (utils/get-element "add-document")]
    (let [file (aget (.-files element) 0)
          name (.-name file)
          title (.-value (utils/get-element "document-title"))
          document-type (get-document-type)]
      (rf/dispatch [::admin/document-add file name title document-type]))))

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

;; generalize this to have multiple documents that can be uploaded
;; individually or all at once
(defn page []
  [:div.app
   [:section.hero
    [:div.hero-body
     [:p.title "Add Documents"]]]
   (if @(rf/subscribe [::subs/token])
     (r/with-let [name (r/atom (get-document-name))]
       (r/with-let [page-numbers (r/atom [0])]
         [:div.box
          (upload-document name)
          (when @name
            [:div
             [:div.block
              [:label.label "Title"]
              [:div.field {:class "has-addons"}
               [:div.control
                [:input.input
                 {:type "text" :id "document-title" :placeholder "Title"}]]
               [:div.control
                [utils/dropdown
                 "document-type" ["article" "book"]
                 :rounded? true :title "document type"]]]]
             [:div.block
              [:label.label "Page Numbering"]
              (reduce
               (fn [result id]
                 (let [last? (= id (dec (count @page-numbers)))]
                   (conj result (page-input id page-numbers last?))))
               [:div]
               (sort < @page-numbers))]
             [:div.block
              (utils/button "upload" (fn []
                                       (println "THERE")
                                       (upload-one!)))]])]))
     [:div.section [login/page]])])
