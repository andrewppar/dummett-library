(ns dummett-library.views.utils)

(defn dropdown [id menu-items & {:keys [rounded? title]}]
  (let [base [:select {:id id}]]
    [:div.select (when rounded? {:class "is-rounded"})
     (reduce
      (fn [acc item]
        (conj
         acc
         [:option {:id (str "dropdown" item)} item]))
      (if title (conj base [:option {:selected true :disabled true} title]) base)
      menu-items)]))

(defn input [type id label & {:keys [placeholder icon]}]
  [:div.block
   [:label {:class "label"} label]
   [:div.control {:class "has-icons-left"}
    [:input.input
     (cond-> {:type (case type
                      :email "email"
                      :password "password"
                      :telephone "tel"
                      "text")
              :id id}
       placeholder (assoc :placeholder placeholder))]
    (when icon
      [:span.icon {:class "is-small is-left"}
       [:i {:class icon :aria-hidden true}]])]])

(defn get-element [id]
  (. js/document (getElementById id)))

(defn button
  [text submit-fn &
   {:keys [type color] :or {type "submit" color :info}}]
  (let [bulma-color (str "is-" (name color))]
    [:button
     {:type type
      :class (str "button " bulma-color)
      :on-click submit-fn}
     text]))

(defn admin? [role]
  (= role "admin"))
