(ns dummett-library.test-parse
  (:require [dummett-library.parse :refer [xml-page->text]]
            [clojure.test :refer [deftest is]]))

(deftest xml-page->text-test
  (is (= (xml-page->text {:tag :html :content [{:tag :p :content ["Some Text"]}]})
         "\nSome Text"))

  (is (= (xml-page->text {:tag :html :content [{:tag :p :content ["Some Text"]}
                                               {:tag :p :content ["More Text"]}]})
         "\nSome Text\nMore Text")))
