(ns dummett-library.admin.user
  (:require
   [clojure.edn :as edn]
   [dummett-library.store.core :as store]
   [dummett-library.store.searcher :as searcher]
   [dummett-library.query.core :as query])
  (:import
   (java.security SecureRandom)
   (javax.crypto.spec PBEKeySpec)
   (javax.crypto SecretKeyFactory)))

(defn make-salt []
  (let [salt-bytes (.generateSeed (SecureRandom.) 16)
        salt-string (str (vec salt-bytes))]
    {:bytes salt-bytes
     :string salt-string}))

(defn generate-password [salt password]
  (let [keyspec (PBEKeySpec. (.toCharArray password) salt 65536, 128)
        factory (SecretKeyFactory/getInstance "PBKDF2WithHmacSHA1")]
    (String. (.getEncoded (.generateSecret factory keyspec)) "UTF-8")))

(defn add! [email password role]
  (let [{salt :bytes salt-string :string} (make-salt)
        pass (generate-password salt password)]
    (if (< 0 (store/add-user! email salt-string pass role))
      {:status 200}
      {:status 500})))

(defn user-docs [email]
  (let [searcher (searcher/make (store/store))
        analyzer (store/analyzer)]
    (query/user searcher analyzer email)))

(defn fetch [email]
  (let [user (user-docs email)]
    (cond (> (count user) 1)
          {:status 500
           :message "More than one user with email"}

          (not (seq user))
          {:status 404
           :message "No such user"}

          :else
          (-> (first user)
              (select-keys [:salt :password :role])
              (assoc :status 200)))))

(defn authenticated?
  [password {salt-string :salt saved-pass :password}]
  (let [salt-bytes (edn/read-string salt-string)
        salt (byte-array 16)]
    (reduce
     (fn [idx salt-byte] (aset-byte salt idx salt-byte) (inc idx))
     0
     salt-bytes)
    (= (generate-password salt password) saved-pass)))

(defn admin? [role]
  (= role "admin"))

(defn retrieve [email password]
  (let [{:keys [status] :as user-data} (fetch email)]
    (if (= status 200)
      (if (authenticated? password user-data)
        (-> user-data
            (select-keys [:status :email :role])
            (assoc :email email))
        {:status 401
         :message "Unauthorized"})
      user-data)))

;; remove
;;

(defn remove! [email]
  (let [remove-count (store/remove-user! email)]
    (if (= remove-count 0)
      {:status 404}
      {:status 200})))

(defn edit!
  [email & {:as updates}]
  (let [docs (map
              (fn [doc]
                (reduce-kv
                 (fn [doc* field value] (assoc doc* field value))
                 doc
                 updates))
              (user-docs email))
        {remove-status :status} (remove! email)]
    (if (= remove-status 404)
      {:status 404}
      (reduce
       (fn [{status :status :as result} {:keys [email salt password role]}]
         (let [docs-added (store/add-user! email salt password role)]
           (assoc result :status (if (> docs-added 0) status 404))))
       {:status 200}
       docs))))

(comment
  (add! "anparisi" "one" "user")
  (fetch "george")
  (fetch "anparisi")
  (authenticated? "one" (fetch "anparisi"))
  (remove! "anparisi")
  (edit! "anparisi" :role "admin")


  )
