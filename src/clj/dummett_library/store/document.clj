(ns dummett-library.store.document
  (:import (org.apache.lucene.document
            Document Field TextField StringField Field$Store)))

(defn ^:private make ^Document
  []
  (Document.))

(defn new-field ^Field
  [field value field-type &
   {:keys [store?] :or {store? true}}]
  (let [field-store-value (if store? Field$Store/YES Field$Store/NO)]
    (case field-type
      :text
      (TextField. field value field-store-value)
      :string
      (StringField. field value field-store-value))))

(defn page
  "Create a new document with author, title, page, and text fields"
  [author title doc-type page text]
  (let [author-field (new-field "author" author :text)
        title-field (new-field "title" title :text)
        doc-type-field (new-field "type" doc-type :string)
        page-field (new-field "page" page :string)
        text-field (new-field "text" text :text)
        document (make)]
    (.add document author-field)
    (.add document title-field)
    (.add document doc-type-field)
    (.add document page-field)
    (.add document text-field)
    document))

(defn user
  "Create a document with an email, password, and role field."
  [email salt password role]
  (doto (make)
    (.add (new-field "email" email :string))
    (.add (new-field "salt" salt :string))
    (.add (new-field "password" password :string))
    (.add (new-field "role" role :string))))
