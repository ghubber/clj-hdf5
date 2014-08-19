(ns clj-hdf5.mdarray-abstract
  (:require [clojure.core.matrix.protocols :as mp])
  (:require [clojure.core.matrix.implementations :as imp])
  (:import  [ch.systemsx.cisd.base.mdarray MDAbstractArray MDByteArray MDDoubleArray MDFloatArray MDIntArray
             MDLongArray MDShortArray]))

(defn value-coerce [m x]
  (let [t (mp/element-type m)]
    (cond
      (= Byte/TYPE t) (byte x)
      (= Double/TYPE t) (double x)
      (= Float/TYPE t) (float x)
      (= Integer/TYPE t) (int x)
      (= Long/TYPE t) (long x)
      (= Short/TYPE t) (short x)
      :else x)))


(defn fn-name-with-suffix
  [fn-name-sym suffix-sym]
  (symbol (str fn-name-sym "-" suffix-sym)))

(defmacro make-concrete
  [implementation-key suffix array-type object-type type-cast is-numerical]
  `(do
     (defn ~(fn-name-with-suffix 'empty-mdarray suffix)
       "Returns an empty implementation of MDAbstractArray of given shape"
       [~'shape]
       (let [~'shape (int-array ~'shape)]
         (new ~array-type ~'shape )))
     (defn ~(fn-name-with-suffix 'mdarray suffix)
       "Returns empty implementation of MDAbstractArray, preserving shape of the data"
       [~'data]
       (cond
         (instance? ~array-type ~'data)
         (new ~array-type (.getCopyAsFlatArray ~'data) (.dimensions ~'data))
         (mp/is-scalar? ~'data)
         (double ~'data)
         :default
         (let [~'mtx (~(fn-name-with-suffix 'empty-mdarray suffix) (mp/validate-shape ~'data))]
           (mp/assign! ~'mtx ~'data)
           ~'mtx)))
     (extend-protocol mp/PImplementation
       ~array-type
       (implementation-key [~'m]
         ~implementation-key)
       (meta-info [~'m]
         {:doc "An implementation of a multi-dimensional array based on CISD's JHDF5 library"})
       (new-vector [~'m ~'length]
         (~(fn-name-with-suffix 'empty-mdarray suffix) [~'length]))
       (new-matrix [~'m ~'rows ~'columns]
         (~(fn-name-with-suffix 'empty-mdarray suffix) [~'rows ~'columns]))
       (new-matrix-nd [~'m ~'shape]
         (~(fn-name-with-suffix 'empty-mdarray suffix) ~'shape))
       (construct-matrix [~'m ~'data]
         (~(fn-name-with-suffix 'mdarray suffix) ~'data))
       (supports-dimensionality? [~'m ~'dims]
         true))

     (extend-protocol mp/PMatrixCloning
       ~array-type
       (clone [~'m]
         (new ~array-type (.getCopyAsFlatArray ~'m) (.dimensions ~'m))))

     (extend-protocol mp/PTypeInfo
       ~array-type
       (element-type [~'m]
         ~object-type))

     (extend-protocol mp/PNumerical
       ~array-type
       (numerical? [~'m]
         ~is-numerical))))

(make-concrete :mdarray-abstract-double double MDDoubleArray Double/TYPE double true)
(make-concrete :mdarray-abstract-float float MDFloatArray Float/TYPE float true)
(make-concrete :mdarray-abstract-int int MDIntArray Integer/TYPE int true)
(make-concrete :mdarray-abstract-long long MDLongArray Long/TYPE long true)
(make-concrete :mdarray-abstract-short short MDShortArray Short/TYPE short true)

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

(extend-protocol mp/PElementCount
  MDAbstractArray
  (element-count [m]
    (.size m)))

(imp/register-implementation (empty-mdarray-double [1]))
(imp/register-implementation (empty-mdarray-float [1]))
(imp/register-implementation (empty-mdarray-int [1]))
(imp/register-implementation (empty-mdarray-long [1]))
(imp/register-implementation (empty-mdarray-short [1]))
