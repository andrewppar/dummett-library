(ns dummett-library.admin.user
  (:require
   [cheshire.core :as json]
   [clojure.edn :as edn]
   [dummett-library.http :as http]
   [dummett-library.query.core :as query]
   [dummett-library.store.core :as store]
   [dummett-library.store.searcher :as searcher])
  (:import
   (java.security SecureRandom)
   (javax.crypto SecretKeyFactory)
   (javax.crypto.spec PBEKeySpec)))

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
      (http/success (format "Created user %s with role %s" email role))
      (http/server-error "Failed to create user"))))

(defn user-docs [email]
  (let [searcher (searcher/make (store/store))
        analyzer (store/analyzer)]
    (query/user searcher analyzer email)))

(defn fetch [email]
  (let [user (user-docs email)]
    (cond (> (count user) 1)
          (http/server-error "More than one user with email")

          (not (seq user))
          (http/not-found "No such user")

          :else
          (-> (first user)
              (select-keys [:salt :password :role])
              http/success))))

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
  (let [{:keys [status body] :as user-data} (fetch email)]
    (if (= status 200)
      (if (authenticated? password (json/parse-string body keyword))
        (-> user-data
            (select-keys [:status :email :role])
            (assoc :email email)
            http/success)
        (http/unauthorized))
      user-data)))

;; remove
;;

(defn remove! [email]
  (let [remove-count (store/remove-user! email)]
    (if (= remove-count 0)
      (http/not-found "User not found")
      (http/success "Removed user"))))

(defn edit!
  [email & {:as updates}]
  ;; this feels too complicated
  (let [docs (map
              (fn [doc]
                (reduce-kv
                 (fn [doc* field value] (assoc doc* field value))
                 doc
                 updates))
              (user-docs email))
        {remove-status :status} (remove! email)]
    (if (= remove-status 404)
      remove-status
      (do
        (run!
         (fn [{:keys [email salt password role]}]
           (store/add-user! email salt password role))
         docs)
        (http/success (format "Updated %s" email))))))

(defn list-all []
  (->> (query/list-users (searcher/make (store/store)) (store/analyzer))
       (mapv (fn [user] (select-keys user [:email :role])))
       http/success))

(comment
  (add! "anparisi" "one" "user")
  (fetch "george")
  (fetch "anparisi")
  (authenticated? "one" (json/parse-string (get (fetch "anparisi") :body) keyword))
  (remove! "anparisi")
  (edit! "anparisi" :role "admin"))
