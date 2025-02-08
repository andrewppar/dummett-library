(ns dummett-library.views.admin.add
  (:require
   [dummett-library.views.login :as login]
   [dummett-library.subs :as subs]
   [re-frame.core :as rf]))

(defn page []
  (let [auth-token @(rf/subscribe [::subs/token])]
    (if auth-token
      [:div.app [:div [:p "addy badmin"]]]
      [:div.section [login/page]])))
