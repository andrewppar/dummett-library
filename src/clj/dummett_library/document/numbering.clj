(ns dummett-library.document.numbering
  (:require [clojure.string :as string]))


(def roman-unit-value
  [["M" 1000] ["CM" 900]
   ["D" 500] ["CD" 400]
   ["C" 100] ["XC" 90]
   ["L" 50] ["XL" 40]
   ["X" 10] ["IX" 9]
   ["V" 5] ["IV" 4]
   ["I" 1]])

(defn greatest-entry-less-than [number]
  (some
   (fn [letter->letter-value]
     (let [letter (first letter->letter-value)
           value (second letter->letter-value)]
       (when (<= value number)
         letter->letter-value)))
   roman-unit-value))

(defn roman-step [{:keys [current] :as acc}]
  (let [[next-letter letter-value] (greatest-entry-less-than current)]
    (-> acc
        (update :result conj next-letter)
        (update :current - letter-value))))

(defn apply-when [condition function value]
  (if condition (function value) value))

(defn ->roman [number & {:keys [lower-case?]}]
  (loop [acc {:result [] :current number}]
    (if (= (get acc :current) 0)
      (->> (get acc :result)
           string/join
           (apply-when lower-case? string/lower-case))
      (recur (roman-step acc)))))
