(ns dummett-library.views.admin.user
  (:require
   [clojure.string :as string]
   [dummett-library.subs :as subs]
   [dummett-library.events.admin :as admin]
   [dummett-library.views.utils :as u]
   [reagent.core :as r]
   [re-frame.core :as rf]))

(defn notification [message]
  [:div.notification {:class "is-success" :id "admin-success"}
   [:button.delete
    {:on-click
     (fn [] (rf/dispatch [::admin/remove-admin-success]))}]
   message])

(defn submit-add-user []
  (let [select (. js/document (getElementById "role-dropdown"))
        email (.-value (. js/document (getElementById "email")))
        password (.-value (. js/document (getElementById "password")))
        options (.-options select)
        idx (.-selectedIndex select)
        role (string/lower-case (.-value (aget options idx)))]
    (rf/dispatch [::admin/add-user email password role])))

(defn add-user []
  [:form.box
   [:h2.title {:class "is-3"} "Add User"]
   (u/input :text "email" "Email"
            :placeholder "email" :icon "fa fa-envelope")
   (u/input :password "password" "Password"
            :placeholder "password" "fa fa-lock")
   [:div.block
    [:label.label "Role"]
    [u/dropdown "role-dropdown" ["User" "Admin"] :rounded? true]
    [:button.button
     {:class "is-success is-rounded fa fa-plus"
      :style {:float "right"}
      :on-click submit-add-user}]]])

(defn user-table [user-list selected-row]
  (let [{:keys [rows]}
        (reduce
         (fn [{:keys [idx] :as acc} {:keys [email role]}]
           (let [row-id (str "update-user-row-" idx)
                 is-selected? (= @selected-row row-id)
                 row [:tr {:id row-id
                           :on-click (fn [] (reset! selected-row row-id))
                           :class (if is-selected? "is-selected" "")}
                      [:td email] [:td role]]]
             (-> acc
                 (update :rows conj row)
                 (update :idx inc))))
         {:rows [:tbody] :idx 0}
         user-list)]
    [:table.table {:class "is-striped is-bordered" :id "user-table"}
     [:thead [:tr [:td "name"] [:td "role"]]] rows]))

(defn update-user []
  (r/with-let [selected-row (r/atom nil)]
    [:div.box
     [:h2.title {:class "is-3"} "Edit User"]
     [:div.columns
      [:div.column
       (rf/dispatch [::admin/fetch-users])
       (let [user-list @(rf/subscribe [::subs/user-list])]
         [:div.block [user-table user-list selected-row]])]
      (when @selected-row
        [:div.column
         [:div.block
          [:label.label "Change Role"]
          [:div.block
           [u/dropdown "update-role-dropdown" ["User" "Admin"] :rounded? true]]
          [:div.block
           [u/button "Update Role"
            (fn []
              (when-let [row (u/get-element @selected-row)]
                (let [email (.-textContent (aget (.-children row) 0))
                      select (u/get-element "update-role-dropdown")
                      idx (.-selectedIndex select)
                      options (.-options select)
                      role (string/lower-case (.-value (aget options idx)))]
                  (reset! selected-row nil)
                  (rf/dispatch [::admin/update-role email role]))))]]]
         [:div.block
          [:label.label "Delete User"]
          [u/button "delete"
           (fn []
             (when-let [row (u/get-element @selected-row)]
               (let [email (.-textContent (aget (.-children row) 0))]
                 (reset! selected-row nil)
                 (rf/dispatch [::admin/delete-user email]))))
           :color :danger]]])]]))

(defn page []
  [:div.app
   [:section.hero
    [:div.hero-body
     [:p.title  "Manage Users"]]]
   (when-let [success-message @(rf/subscribe [::subs/admin-success])]
     [notification success-message])
   (let [auth-token @(rf/subscribe [::subs/token])
         role @(rf/subscribe [::subs/role])]
     (if (and auth-token true #_(u/admin? role))
       [:div
        [add-user]
        [update-user]]
       [:div
        [:h3.title {:class "is-3"} "Unauthorized"]]))])
