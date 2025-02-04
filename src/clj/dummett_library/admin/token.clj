(ns dummett-library.admin.token
  (:require
   [cheshire.core :as json]
   [clojure.string :as string]
   [dummett-library.admin.user :as user]
   [dummett-library.http :as http])
  (:import
   (io.jsonwebtoken SignatureAlgorithm Jwts)
   (java.time Duration)
   (java.util Date)
   (java.util Base64)
   (javax.crypto.spec SecretKeySpec)))

(def secret-key (System/getenv "SECRET_KEY"))

(def hmacKey
  (SecretKeySpec.
   (.decode (Base64/getDecoder) secret-key)
   (.getJcaName SignatureAlgorithm/HS256)))

(defn token [email password]
  (let [{:keys [status body] :as user-data} (user/fetch email)
        parsed-body (json/parse-string body keyword)]
    (if (> status 300)
      user-data
      (if (user/authenticated? password parsed-body)
        (let [builder (Jwts/builder)
              now (.toInstant (Date. (System/currentTimeMillis)))
              exp (.plus now (Duration/ofMinutes 30))]
          (.claim builder "email" email)
          (.claim builder "role" (get parsed-body :role))
          (.setSubject builder email)
          (.setId builder (str (random-uuid)))
          (.setIssuedAt builder (Date/from now))
          (.setExpiration builder (Date/from exp))
          (.signWith builder hmacKey)
          (http/success {:token (.compact builder) :email email}))
        (http/unauthorized)))))

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
        (http/success
         {:header (.getHeader parser)
          :body (.getBody parser)
          :signature (.getSignature parser)}))
      (catch io.jsonwebtoken.ExpiredJwtException _err
        (http/make-response 401 {:status :expired})))))

(defmethod parse :basic
  [auth-string]
  (let [matches (re-matches #"^(?i)basic (.*)$" auth-string)
        token (second matches)
        parts (-> (Base64/getDecoder)
                  (.decode token)
                  (slurp "UTF-8")
                  (string/split #":"))]
    (if (= (count parts) 2)
      (http/success
       {:body {"email" (first parts)
               "password" (second parts)}})
      (http/make-response 401 {:status :unauthorized}))))

(defmethod parse :default
  [_auth-string]
  (http/bad-request))
