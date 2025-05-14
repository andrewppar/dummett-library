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

(defn get-document-numbering [page-numbers]
  (mapv
   (fn [id]
     (let [dropdown (utils/get-element (str "numbering-dropdown-" id))
           options (.-options dropdown)
           idx (.-selectedIndex dropdown)
           numbering-style (.-value (aget options idx))
           slice-number (str "slice-index-" id)
           page-number (str "page-number-" id)]
       {:slice-number (.-value (utils/get-element slice-number))
        :page-number (.-value (utils/get-element page-number))
        :style numbering-style}))
   @page-numbers))

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

(defn upload-one! [page-numbers]
  (when-let [element (utils/get-element "add-document")]
    (let [file (aget (.-files element) 0)
          name (.-name file)
          title (.-value (utils/get-element "document-title"))
          document-type (get-document-type)
          numbering (get-document-numbering page-numbers)]
      (rf/dispatch [::admin/document-add file name title document-type numbering]))))

(defn page-input [id page-numbers last?]
  [:div.field {:class "has-addons"}
   [:div.control
    [utils/dropdown
     (str "numbering-dropdown-" id) ["1" "i" "I"]
     :rounded? true :title "numbering style"]]
   [:div.control
    [:input.input {:id (str "page-number-" id)
                   :type "text"
                   :title "This is the page number printed on the page that starts this range. For example, this could be 245 even when this is the first page of the document being uploaded."
                   :placeholder "page number"}]]
   [:div.control
    [:input.input {:id (str "slice-index-" id)
                   :type "text"
                   :title "This is the page number of the document (usually pdf) itself that this numbering style starts on. For example, PDFs of articles will often have this number be 1 (the start of the document itself) while the page number is some large number representing the printed page number, e.g. 245, in the journal edition itself."
                   :placeholder "slice index"}]]
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
              [:label.label "Page Numbering "
               [:i {:class "fa fa-question-circle"
                    :title "Use this field to input the page numbering scheme for the document. The numbering scheme is divided into slices - each slice represents a way of numbering pages for the document. For example, a book that has an Introduction with lower case roman numerals (i) starting on the logical page 3 and arabic numerals (1) starting on page 7 has two slices. The first slice is specified as \"i, 3, 1\" and the second as \"1, 7, 1\". Where the first component is the numbering style, the second component is where the slice starts in the document being uploaded, and the third part is the number that is visible on the first page of that slice in the document.\n\n There are defaults for all those positions: If no slices are specified, then one slice is assumed. That slices number is assumed to be arabic (1), starting on the first logical page, with 1 as the visible page number, i.e. the slice \"1, 1, 1\" is assumed. Those are also the default values for any slice where they are not specified."}]]
              (reduce
               (fn [result id]
                 (let [last? (= id (dec (count @page-numbers)))]
                   (conj result (page-input id page-numbers last?))))
               [:div]
               (sort < @page-numbers))]
             [:div.block
              (utils/button "upload" (fn [] (upload-one! page-numbers)))]])]))
     [:div.section [login/page]])])
