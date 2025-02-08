(ns dummett-library.views.utils)

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
