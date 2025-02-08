(ns dummett-library.http
  (:require [cheshire.core :as json]))

(defn make-response [status body]
  (let [response-body (if (string? body) {:message body} body)]
    {:status status :body (json/generate-string response-body)}))

(defn not-authorized [& {:keys [body]}]
  (let [body-message (or body "not authorized")]
    (make-response 401 body-message)))

(defn success [body]
  (make-response 200 body))

(defn bad-request [& {:keys [body]}]
  (if body
    (make-response 400 body)
    (make-response 400 "")))

(defn unauthorized [& {:keys [body]}]
  (let [body-message (or body "Unauthorized")]
    (make-response 401 body-message)))

(defn not-found [body]
  (make-response 404 body))

(defn server-error [body]
  (make-response 500 body))
