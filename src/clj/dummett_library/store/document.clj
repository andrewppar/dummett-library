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
  (doto (make)
    (.add (new-field "type" "page" :string))
    (.add (new-field "author" author :text))
    (.add (new-field "title" title :text))
    (.add (new-field "document-type" doc-type :string))
    (.add (new-field "page" page :string))
    (.add (new-field "text" text :text))))

(defn library-record
  "Create a document that records the presence and metadata of another
  document."
  ;; these have repeated fields - probably they should join on a document id
  [author title document-type archivist]
  (doto (make)
    (.add (new-field "type" "record" :string))
    (.add (new-field "document-author" author :string))
    (.add (new-field "document-title" title :string))
    (.add (new-field "document-type" document-type :string))
    (.add (new-field "archivist" archivist :string))))

(defn user
  "Create a document with an email, password, and role field."
  [email salt password role]
  (doto (make)
    (.add (new-field "type" "user" :string))
    (.add (new-field "email" email :string))
    (.add (new-field "salt" salt :string))
    (.add (new-field "password" password :string))
    (.add (new-field "role" role :string))))
