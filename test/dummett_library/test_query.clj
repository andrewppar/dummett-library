(ns dummett-library.test-query
  (:require [dummett-library.query :refer [new-query]]
            [dummett-library.analyze :refer [new-analyzer]]
            [clojure.test :refer [deftest is]]))

(deftest new-query-test
  (is (= (str (new-query (new-analyzer) "kant"  ["article"])) "+text:kant +(type:article)"))

  (is (= (str (new-query (new-analyzer) "this text" ["book" "article"]))
         "+(text:this text:text) +(type:book type:article)"))

  (is (thrown? Exception (str (new-query (new-analyzer) "" [])))))
