(ns clj-hdf5.mdarray
  (:require [clojure.core.matrix.protocols :as mp])
  (:require [clojure.core.matrix.implementations :as imp])
  (:import  [ch.systemsx.cisd.base.mdarray MDAbstractArray MDDoubleArray]))

(defn value-coerce [m x]
  (let [t (mp/element-type m)]
    (if (= Double/TYPE t)
      (double x))))

(def impl-infos
  { :double { :implementation-key :mdarray-double
              :suffix 'double
              :array-type 'MDDoubleArray
              :object-type Double/TYPE }

    })

(defmacro make-concrete
  [info body]
  (let [{implementation-key :implementation-key
         suffix :suffix
         array-type :array-type
         object-type :object-type} info]
  `(do
    (defn empty-mdarray-'suffix
      "Returns an empty MDDoubleArray of given shape"
      [~'shape]
      (let [~'shape (int-array ~'shape)]
        (new ~array-type ~'shape )))

    (defn mdarray-'suffix
      "Returns MDArrayDouble with given data, preserving shape of the data"
      [~'data]
      (cond
        (instance? ~array-type ~'data)
        (new ~array-type (.getCopyAsFlatArray data) (.dimensions ~'data))
        (mp/is-scalar? ~'data)
        (double data)
        :default
        (let [~'mtx (empty-mdarray-'suffix (mp/validate-shape ~'data))]
          (mp/assign! ~'mtx ~'data)
          ~'mtx)))

    (extend-protocol mp/PImplementation
      ~'array-type
      (implementation-key [m]
        ~implementation-key)
      (meta-info [m]
        {:doc "An implementation of a multi-dimensional array based on CISD's JHDF5 library"})
      (new-vector [m length]
        (empty-mdarray-'suffix [length]))
      (new-matrix [m rows columns]
        (empty-mdarray-'suffix [rows columns]))
      (new-matrix-nd [m shape]
        (empty-mdarray-'suffix shape))
      (construct-matrix [m data]
        (mdarray-'suffix data))
      (supports-dimensionality? [m dims]
        true)))))

(make-concrete impl-infos :double)

(comment
(defn empty-mdarray-double
  "Returns an empty MDDoubleArray of given shape"
  [shape]
  (let [shape (int-array shape)]
    (new MDDoubleArray shape )))

(defn mdarray-double
  "Returns MDArrayDouble with given data, preserving shape of the data"
  [data]
  (cond
    (instance? MDDoubleArray data)
      (new MDDoubleArray (.getCopyAsFlatArray data) (.dimensions data))
    (mp/is-scalar? data)
      (double data)
    :default
      (let [mtx (empty-mdarray-double (mp/validate-shape data))]
        (mp/assign! mtx data)
        mtx)))

(extend-protocol mp/PImplementation
  MDDoubleArray
  (implementation-key [m]
    :mdarray-double)
  (meta-info [m]
    {:doc "An implementation of a multi-dimensional array based on CISD's JHDF5 library"})
  (new-vector [m length]
    (empty-mdarray-double [length]))
  (new-matrix [m rows columns]
    (empty-mdarray-double [rows columns]))
  (new-matrix-nd [m shape]
    (empty-mdarray-double shape))
  (construct-matrix [m data]
    (mdarray-double data))
  (supports-dimensionality? [m dims]
    true)))

(extend-protocol mp/PDimensionInfo
  MDAbstractArray
  (dimensionality [m]
    (count (.dimensions m)))
  (get-shape [m]
    (.dimensions m))
  (is-scalar? [m]
    false)
  (is-vector? [m]
    (== 1 (count (.dimensions m))))
  (dimension-count [m x]
    (.size m (int x))))

(extend-protocol mp/PIndexedAccess
  MDAbstractArray
  (get-1d [m x]
    (.getAsObject m (int x)))
  (get-2d [m x y]
    (.getAsObject m (int-array [x y])))
  (get-nd [m indices]
    (.getAsObject m (int-array indices))))

(extend-protocol mp/PIndexedSetting
  MDAbstractArray
  (set-1d [m row v]
    (let [m-new (mp/clone m)]
      (mp/set-1d! m-new row v)
      m-new))
  (set-2d [m row column v]
    (let [m-new (mp/clone m)]
      (mp/set-2d! m-new row column v)
      m-new))
  (set-nd [m indices v]
    (let [m-new (mp/clone m)]
      (mp/set-nd! m-new indices v)
      m-new))
  (is-mutable? [m]
    true))

(extend-protocol mp/PIndexedSettingMutable
  MDAbstractArray
  (set-1d! [m x v]
    (when-not (== 1 (mp/dimensionality m))
      (throw (IllegalArgumentException. "can't use set-1d! on non-vector")))
    (.setToObject m (value-coerce m v) (int x)))
  (set-2d! [m x y v]
    (when-not (== 2 (mp/dimensionality m))
      (throw (IllegalArgumentException. "can't use set-2d! on non-matrix")))
    (.setToObject m (value-coerce m v) (int-array [x y])))
  (set-nd! [m indices v]
    (when-not (= (count indices) (mp/dimensionality m))
      (throw (IllegalArgumentException.
               "index count should match dimensionality")))
    (.setToObject m (value-coerce m v) (int-array indices))))

(extend-protocol mp/PMatrixCloning
  MDDoubleArray
  (clone [m]
    (new MDDoubleArray (.getCopyAsFlatArray m) (.dimensions m))))

(extend-protocol mp/PTypeInfo
  MDDoubleArray
  (element-type [m]
    (Double/TYPE)))

(extend-protocol mp/PNumerical
  MDDoubleArray
  (numerical? [m]
    true))

(extend-protocol mp/PElementCount
  MDAbstractArray
  (element-count [m]
    (.size m)))

(imp/register-implementation (empty-mdarray-double [1]))
