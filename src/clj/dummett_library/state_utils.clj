(ns dummett-library.state-utils)

(defn get-env
  "Get the value of `env-var` or supply default."
  ([env-var]
   (get-env env-var nil))
  ([env-var default]
   (or (System/getenv env-var) default)))

(defn setc
  ([config config-key]
   (setc config config-key (eval (get-in @config [config-key :default]))))
  ([config config-key value]
   (swap! config assoc-in [config-key :value] value)))

(defn getc [config config-key]
  (or (get-in @config [config-key :value])
      (do
        (setc config config-key)
        (get-in @config [config-key :value]))))
