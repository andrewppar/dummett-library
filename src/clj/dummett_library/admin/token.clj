(ns dummett-library.admin.token
  (:require
   [clojure.string :as string]
   [dummett-library.admin.user :as user])
  (:import
   (io.jsonwebtoken SignatureAlgorithm Jwts)
   (java.util Date)
   (java.util Base64)
   (java.time Duration)
   (javax.crypto.spec SecretKeySpec)))

(def secret-key (System/getenv "SECRET_KEY"))

(def hmacKey
  (SecretKeySpec.
   (.decode (Base64/getDecoder) secret-key)
   (.getJcaName SignatureAlgorithm/HS256)))

(defn token [email password]
  (let [user-data (user/fetch email)]
    (if (> (get user-data :status) 300)
      user-data
      (if (user/authenticated? password user-data)
        (let [builder (Jwts/builder)
              now (.toInstant (Date. (System/currentTimeMillis)))
              exp (.plus now (Duration/ofMinutes 30))]
          (.claim builder "email" email)
          (.claim builder "role" (get user-data :role))
          (.setSubject builder email)
          (.setId builder (str (random-uuid)))
          (.setIssuedAt builder (Date/from now))
          (.setExpiration builder (Date/from exp))
          (.signWith builder hmacKey)
          {:body {:token (.compact builder)}
           :status 200})
        {:body {:token nil}
         :status 401}))))

(defn parse-dispatch [auth-string]
  (let [lower-auth (string/lower-case auth-string)]
    (some
     (fn [auth-type]
       (when (string/starts-with? lower-auth auth-type)
         (keyword auth-type)))
     ["bearer" "basic"])))

(defmulti parse parse-dispatch)

(defmethod parse :bearer
  [auth-string]
  (let [matches (re-matches #"^(?i)bearer (.*)$" auth-string)
        token (second matches)
        parser-builder (Jwts/parser)]
    (.setSigningKey parser-builder hmacKey)
    (try
      (let [parser (.parseClaimsJws (.build parser-builder) token)]
        {:status :ok
         :header (.getHeader parser)
         :body (.getBody parser)
         :signature (.getSignature parser)})
      (catch io.jsonwebtoken.ExpiredJwtException _err
        {:status :expired}))))

(defmethod parse :basic
  [auth-string]
  (let [matches (re-matches #"^(?i)basic (.*)$" auth-string)
        token (second matches)
        parts (-> (Base64/getDecoder)
                  (.decode token)
                  ;;; this doesn't work
                  (slurp "UTF-8")
                  (string/split #":"))]
    (if (= (count parts) 2)
      {:status 200
       :body {"email" (first parts)
              "password" (second parts)}}
      {:status 401})))

(defmethod parse :default
  [auth-string]
  {:status 400
   :token auth-string})
