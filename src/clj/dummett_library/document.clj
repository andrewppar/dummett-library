(ns dummett-library.document
  (:import (org.apache.lucene.document
            Document Field TextField StringField Field$Store FieldType)))

(def text-with-position-type
  (let [type (FieldType. TextField/TYPE_NOT_STORED)]
    (.setStoreTermVectors type true)
    (.setStoreTermVectorPositions type true)
    type))

(defn new-document ^Document
  []
  (Document.))

(defn new-field ^Field
  [field value field-type &
   {:keys [store?] :or {store? true}}]
  (let [field-store-value (if store? Field$Store/YES Field$Store/NO)]
    (case field-type
      :with-position
      (text-with-position-type field value field-store-value)
      :text
      (TextField. field value field-store-value)
      :string
      (StringField. field value field-store-value))))

(defn new-page
  "Create a new document with author, title, page, and text fields"
  [author title doc-type page text]
  (let [author-field   (new-field "author" author :text)
        title-field    (new-field "title"  title  :text)
        doc-type-field (new-field "type" doc-type :string)
        page-field     (new-field "page"   page   :string)
        text-field     (new-field "text"   text   :text)
        document       (new-document)]
    (.add document author-field)
    (.add document title-field)
    (.add document doc-type-field)
    (.add document page-field)
    (.add document text-field)
    document))
    
