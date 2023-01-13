(ns dummett-library.test-document
  (:require [dummett-library.document :refer [new-document]]
            [clojure.test :refer [deftest is]])
  (:import
   (org.apache.lucene.document
            Document )))



(deftest new-document-test
  (is (= (type (new-document)) Document)))
