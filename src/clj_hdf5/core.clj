(ns clj-hdf5.core
  (:refer-clojure :exclude [read name])
  (:require clojure.string)
  (:require [clojure.reflect :as r])
  (:use [clojure.pprint :only [print-table]])
  (:import (java.io.File)
           (ch.systemsx.cisd.hdf5 HDF5Factory IHDF5SimpleReader
                                  IHDF5SimpleWriter HDF5FactoryProvider
                                  HDF5DataClass HDF5StorageLayout)))

; Record definitions
; A node is defined by its reader/writer and its path inside that file.
(defrecord hdf-node
  [accessor path])

(defn make-hdf-node
  [accessor path]
  (new hdf-node accessor path))

; An attribute is defined by its node and its name.
(defrecord hdf-attribute
  [accessor path attrname])

(defn make-hdf-attribute
  [accessor path attrname]
  (new hdf-attribute accessor path attrname))

; Private utility definitions 
(defn- absolute-path?
  [path]
  (= (first path) \/))

(defn- relative-path?
  [path]
  (not (absolute-path? path)))

(defn- path-concat
  [abs-path rel-path]
  (assert (absolute-path? abs-path))
  (assert (relative-path? rel-path))
  (if (= abs-path "/")
    (str "/" rel-path)
    (str abs-path "/" rel-path)))

(def ^{:private true} byte-array-class
     (class (make-array Byte/TYPE 0)))
(def ^{:private true} short-array-class
     (class (make-array Short/TYPE 0)))
(def ^{:private true} int-array-class
     (class (make-array Integer/TYPE 0)))
(def ^{:private true} long-array-class
     (class (make-array Long/TYPE 0)))
(def ^{:private true} float-array-class
     (class (make-array Float/TYPE 0)))
(def ^{:private true} double-array-class
     (class (make-array Double/TYPE 0)))
(def ^{:private true} string-array-class
     (class (make-array java.lang.String 0)))

(def ^{:private true} integer-array-class
     (class (make-array Integer 0)))

; Type checks

(defn node?
  [object]
  (isa? (class object) hdf-node))

(defn dataset?
  [object]
  (and (node? object)
       (. (:accessor object)  isDataSet (:path object))))

(defn datatype?
  [object]
  (and (node? object)
       (. (:accessor object)  isDataType (:path object))))

(defn external-link?
  [object]
  (and (node? object)
       (. (:accessor object)  isExternalLink (:path object))))

(defn group?
  [object]
  (and (node? object)
       (. (:accessor object)  isGroup (:path object))))

(defn soft-link?
  [object]
  (and (node? object)
       (. (:accessor object)  isSoftLink (:path object))))

(defn symbolic-link?
  [object]
  (and (node? object)
       (. (:accessor object)  isSymbolicLink (:path object))))

(defn root?
  [object]
  (and (group? object)
       (= (:path object) "/")))

(defn attribute?
  [object]
  (isa? (class object) hdf-attribute))

; Opening and closing files.
; 

(defn open
  "The return value of open/create is the root group object."
  ([file] (open file :read-only))
  ([file mode]
     (assert (isa? (class file) java.io.File))
     (let [factory (HDF5FactoryProvider/get)]
       (new hdf-node
            (case mode
                  :read-only   (. factory openForReading file)
                  :read-write  (. factory open file)
                  :create      (let [configurator (. factory configure file)]
                                 (. configurator overwrite)
                                 (. configurator writer)))
            "/"))))

(defn create
  [file]
  (open file :create))

(defn close
  [root-group]
  (assert (root? root-group))
  (. (:accessor root-group) close))

; Datatypes

(defn datatype
  [object]
  (assert (or (dataset? object)
              (attribute? object)))
  (let [acc  (:accessor object)
        path (:path object)]
    (if (dataset? object)
      (.getTypeInformation (.getDataSetInformation acc path))
      (.getAttributeInformation acc path (:attrname object)))))

; Reading datasets and attributes

(defmulti read class)

; Nodes

(defn file
  [node]
  (assert (node? node))
  (. (:accessor node) getFile))

(defn path
  [node]
  (assert (node? node))
  (:path node))

(defn name
  [node]
  (last (clojure.string/split (path node) #"/")))

(defn parent
  [node]
  (assert (node? node))
  (let [path (clojure.string/split (:path node) #"/")]
    (if (empty? path)
      nil
      (let [parent-path (subvec path 0 (dec (count path)))]
        (new hdf-node
             (:accessor node)
             (if (= (count parent-path) 1)
               "/"
               (clojure.string/join "/" parent-path)))))))

(defn root
  [node]
  (assert (node? node))
  (new hdf-node (:accessor node) "/"))

(defn level
  [node]
  (max 0 (dec (count (clojure.string/split (path node) #"/")))))

(defn attributes
  [node]
  (assert (node? node))
  (let [acc   (:accessor node)
        path  (:path node)
        names (. acc  getAttributeNames path)]
    (into {} (for [n names]
               [n (new hdf-attribute acc path n)]))))

(defn get-attribute
  [node name]
  (assert (node? node))
  (let [acc  (:accessor node)
        path (:path node)]
    (if (. acc hasAttribute path name)
      (new hdf-attribute acc path name)
      nil)))

(defn read-attribute
  [node name]
  (if-let [attr (get-attribute node name)]
    (read attr)
    nil))

(defn- read-scalar-attribute
  [acc path name dclass]
  (cond
   (= dclass HDF5DataClass/STRING)
      (. acc getStringAttribute path name)
   (= dclass HDF5DataClass/INTEGER)
      (. acc getLongAttribute path name)
   (= dclass HDF5DataClass/FLOAT)
      (. acc getDoubleAttribute path name)
   (= dclass HDF5DataClass/REFERENCE)
      (new hdf-node acc  (. acc getObjectReferenceAttribute path name))
   :else
      nil))

(defn- read-array-attribute
  [acc path name dclass]
  (cond
   (= dclass HDF5DataClass/STRING)
      (vec (. acc getStringArrayAttribute path name))
   (= dclass HDF5DataClass/INTEGER)
      (vec (. acc getLongArrayAttribute path name))
   (= dclass HDF5DataClass/FLOAT)
      (vec (. acc getDoubleArrayAttribute path name))
   :else
      nil))

(defmethod read hdf-attribute
  [attr]
  (let [acc    (:accessor attr)
        path   (:path attr)
        name   (:attrname attr)
        dt     (datatype attr)
        dclass (. dt getDataClass)
        ddims  (vec (. dt getDimensions))]
    (cond
     (not (.isArrayType dt))
        (read-scalar-attribute acc path name dclass)
     (> (count ddims) 1)
        (throw (Exception. "attributes with rank > 1 not implemented yet"))
     :else
         (read-array-attribute acc path name dclass))))

(defmulti create-attribute (fn [node name value] (class value)))

(defmacro ^{:private true} create-attribute-method
  [datatype method-name]
  `(defmethod ~'create-attribute ~datatype
     [~'node ~'name ~'value]
     (let [~'acc  (:accessor ~'node)
           ~'path (:path ~'node)]
       (~method-name ~'acc ~'path ~'name ~'value)
       (new ~'hdf-attribute ~'acc ~'path ~'name))))

(create-attribute-method java.lang.String .setStringAttribute)
(create-attribute-method string-array-class .setStringArrayAttribute)
(create-attribute-method java.lang.Integer .setIntAttribute)
(create-attribute-method int-array-class .setIntArrayAttribute)
(create-attribute-method integer-array-class .setIntArrayAttribute)

(defmethod create-attribute clojure.lang.Sequential
  [node name value]
  (let [el-class (get {Boolean Boolean/TYPE
                       Byte    Byte/TYPE
                       Short   Short/TYPE
                       Integer Integer/TYPE
                       Long    Long/TYPE
                       Float   Float/TYPE
                       Double  Double/TYPE} (class (first value)))]
    (create-attribute node name
                      (if el-class
                        (into-array el-class value)
                        (into-array value)))))

; Groups

(defn members
  [group]
  (assert (group? group))
  (into {}
        (for [name (. (:accessor group) getAllGroupMembers (:path group))]
          [name (new hdf-node
                     (:accessor group)
                     (path-concat (:path group) name))])))

(defn lookup
  [group name]
  (assert (group? group))
  (let [acc       (:accessor group)
        path      (:path group)
        full-path (path-concat path name)]
    (if (. acc exists full-path)
      (new hdf-node acc full-path)
      nil)))

(defn get-dataset
  [root abs-path]
  (assert (root? root))
  (assert (absolute-path? abs-path))
  (let [acc (:accessor root)]
    (if (. acc exists abs-path)
      (new hdf-node acc abs-path)
      nil)))

(defn create-group
  "Creates one (or multiple) child group(s) for a given parent group. The name must be
  a relative path. Note: All intermediate groups will be created as well, if they do not already exist."
  [parent name]
  (assert (group? parent))
  (. (:accessor parent) createGroup (path-concat (:path parent) name))
  (lookup parent name))

; Walk over nodes

(defn walk
  ([node f]
   (walk node f (constantly true)))
  ([node f descend?]
   (lazy-seq
     (cons (f node)
           (when (and (group? node)
                      (descend? node))
             (apply concat (for [n (vals (members node))]
                             (walk n f descend?))))))))

; Datasets

(defn dimensions
  [dataset]
  (assert (dataset? dataset))
  (vec (. (. (:accessor dataset) getDataSetInformation (:path dataset))
          getDimensions)))

(defn max-dimensions
  [dataset]
  (assert (dataset? dataset))
  (vec (. (. (:accessor dataset) getDataSetInformation (:path dataset))
          getMaxDimensions)))

(defn rank
  [dataset]
  (assert (dataset? dataset))
  (. (. (:accessor dataset) getDataSetInformation (:path dataset))
     getRank))

(defn chunked?
  [dataset]
  (assert (dataset? dataset))
  (= HDF5StorageLayout/CHUNKED (. (. (:accessor dataset) getDataSetInformation (:path dataset))
     getStorageLayout)))

(defmulti create-scalar-dataset
          (fn [parent name dt] dt))

(defmulti create-array-dataset
          (fn [parent name size dt] dt))

(defmulti create-matrix-dataset
          (fn [parent name size-x size-y dt] dt))

(defmulti create-mdarray-dataset
          (fn [parent name shape dt] dt))

(defmacro ^{:private true} create-dataset-method
  [datatype writer-method-name]
  `(do
     (defmethod ~'create-scalar-dataset ~datatype
       [~'parent ~'name ~'datatype]
       (assert (group? ~'parent))
       (assert (string? ~'name))
       (let [~'acc       (:accessor ~'parent)
             ~'path      (:path ~'parent)
             ~'full-path (path-concat ~'path ~'name)]
         (.write (~writer-method-name ~'acc) ~'full-path)
         (new ~'hdf-node ~'acc ~'full-path)))
     (defmethod ~'create-array-dataset ~datatype
       [~'parent ~'name ~'size ~'datatype]
       (let [~'acc       (:accessor ~'parent)
             ~'path      (:path ~'parent)
             ~'full-path (path-concat ~'path ~'name)]
         (.createArray (~writer-method-name ~'acc) ~'full-path  ~'size)
         (new ~'hdf-node ~'acc ~'full-path)))
     (defmethod ~'create-matrix-dataset ~datatype
       [~'parent ~'name ~'size-x ~'size-y ~'datatype]
       (assert (group? ~'parent))
       (assert (string? ~'name))
       (let [~'acc       (:accessor ~'parent)
             ~'path      (:path ~'parent)
             ~'full-path (path-concat ~'path ~'name)]
         (.createMatrix (~writer-method-name ~'acc) ~'full-path  ~'size-x ~'size-y)
         (new ~'hdf-node ~'acc ~'full-path)))
     (defmethod ~'create-mdarray-dataset ~datatype
                [~'parent ~'name ~'shape ~'datatype]
       (assert (group? ~'parent))
       (assert (string? ~'name))
       (let [~'acc       (:accessor ~'parent)
             ~'path      (:path ~'parent)
             ~'full-path (path-concat ~'path ~'name)]
         (.createMDArray (~writer-method-name ~'acc) ~'full-path (int-array ~'shape))
         (new ~'hdf-node ~'acc ~'full-path)))))

(create-dataset-method
  Byte .byte )
(create-dataset-method
  Short .int16 )
(create-dataset-method
  Integer .int32 )
(create-dataset-method
  Long .long)
(create-dataset-method
  Float .float32)
(create-dataset-method
  Double .double)
;;(create-dataset-method
;;  String .writeString .writeStringArray .string String string-array-class)

(defmulti read-scalar-dataset
          (fn [acc full-path dt] dt))

(defmulti read-array-dataset
          (fn [acc full-path dt] dt))

(defmulti read-matrix-dataset
          (fn [acc full-path dt] dt))

(defmulti read-mdarray-dataset
          (fn [acc full-path dt] dt))

(defmacro ^{:private true} read-dataset-method
  [datatype reader-method-name]
  `(do
     (defmethod ~'read-scalar-dataset ~datatype
                [~'acc ~'full-path ~'datatype]
         (.read (~reader-method-name ~'acc) ~'full-path)
         (new ~'hdf-node ~'acc ~'full-path))
     (defmethod ~'read-array-dataset ~datatype
                [~'acc ~'full-path ~'datatype]
         (.readArray (~reader-method-name ~'acc) ~'full-path)
         (new ~'hdf-node ~'acc ~'full-path))
     (defmethod ~'read-matrix-dataset ~datatype
                [~'acc ~'full-path ~'datatype]
         (.readMatrix (~reader-method-name ~'acc) ~'full-path)
         (new ~'hdf-node ~'acc ~'full-path))
     (defmethod ~'read-mdarray-dataset ~datatype
                [~'acc ~'full-path ~'datatype]
         (.readMDArray (~reader-method-name ~'acc) ~'full-path)
         (new ~'hdf-node ~'acc ~'full-path))))

(read-dataset-method
  Byte/TYPE  .byte)
(read-dataset-method
  Short/TYPE  .int16)
(read-dataset-method
  Integer/TYPE  .int32)
(read-dataset-method
  Long/TYPE  .long)
(read-dataset-method
  Float/TYPE .float32)
(read-dataset-method
  Double/TYPE  .double)
;;(read-dataset-method
;;  String .readString .string String)

(defn- get-java-type
  "Gets the appropriate Java type of a dataset object. While the HDF5DataClass of a dataset might
  be FLOAT, the dataset might contain double precision floats (doubles) instead. This method would
  return Double/TYPE in such a case. Note that in Java, Double/TYPE is equivalent to double.class,
  so we're talking about primitive types here."
  [ds]
  (assert (dataset? ds))
  (let [dt (datatype ds)]
    (.tryGetJavaType dt)))

(defn print-type-info
  [t]
  (print-table
    (sort-by :name
             (filter :exception-types (:members (r/reflect t))))))

(defmethod read hdf-node
  [ds]
  (assert (dataset? ds))
  (let [acc     (:accessor ds)
        path    (:path ds)
        dsinfo  (.getDataSetInformation acc path)
        rank    (count (.getDimensions dsinfo))
        jt      (get-java-type ds)]
    (cond
     (= rank 0)
        (read-scalar-dataset acc path jt)
     (= rank 1)
        (read-array-dataset acc path jt)
     (= rank 2)
        (read-matrix-dataset acc path jt)
     :else
        (read-mdarray-dataset acc path jt))))


(defn create-dataset
  "Creates a dataset node for the given group and returns it."
  [group name shape dt]
  (assert (group? group))
  (assert (string? name))
  (let [ acc (:accessor group)
         rank (if (coll? shape)
               (count shape)
               0)
         path (path-concat (:path group) name)]
    (do
    (cond
      (= rank 0)
        (create-scalar-dataset group name dt)
      (= rank 1)
        (create-array-dataset group name (int (first shape)) dt)
      (= rank 2)
        (create-matrix-dataset group name (int (first shape)) (int (second shape)) dt)
      :else
        (create-mdarray-dataset group name shape dt))
    (new hdf-node (:accessor group) path ))))
