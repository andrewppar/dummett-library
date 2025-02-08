(ns dummett-library.admin.core
  (:require
   [cheshire.core :as json]
   [dummett-library.admin.user :as user]))

(defn init! []
  (let [{:keys [body]} (user/list-all)
        users (json/decode body keyword)]
    (when (and (not (some (comp user/admin? :role) users))
               true ;;local project
               (System/getenv "ADMIN_USERNAME")
               (System/getenv "ADMIN_PASSWORD"))
      (user/add! (System/getenv "ADMIN_USERNAME")
                 (System/getenv "ADMIN_PASSWORD")
                 "admin"))))
