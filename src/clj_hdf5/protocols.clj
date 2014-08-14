(ns clj-hdf5.protocols
  (:require [clojure.core.matrix :as m]
            [clojure.core.matrix.protocols :as mp]
            [clojure.core.matrix.implementations :as imp])
  (:import (java.io.File)
           (ch.systemsx.cisd.base.mdarray MDByteArray
                                          MDDoubleArray MDDoubleArray
                                          MDFloatArray MDIntArray
                                          MDLongArray MDShortArray)))

(extend-protocol mp/DimensionInfo
  HDF5TimeDurationMDArray
    (dimensionality [m]
      (.dimensions m))
    (is-vector? [m]
      (== 1 (.dimensions m)))
    (dimension-count [m x]
      (.size m (int x)))
  MDByteArray
    (dimensionality [m]
      (.dimensions m))
    (is-vector? [m]
      (== 1 (.dimensions m)))
    (dimension-count [m x]
      (.size m (int x)))
  MDDoubleArray
    (dimensionality [m]
      (.dimensions m))
    (is-vector? [m]
      (== 1 (.dimensions m)))
    (dimension-count [m x]
      (.size m (int x)))
  MDFloatArray
    (dimensionality [m]
      (.dimensions m))
    (is-vector? [m]
      (== 1 (.dimensions m)))
    (dimension-count [m x]
      (.size m (int x)))
  MDIntArray
    (dimensionality [m]
      (.dimensions m))
    (is-vector? [m]
      (== 1 (.dimensions m)))
    (dimension-count [m x]
      (.size m (int x)))
  MDLongArray
    (dimensionality [m]
      (.dimensions m))
    (is-vector? [m]
      (== 1 (.dimensions m)))
    (dimension-count [m x]
      (.size m (int x)))
  MDShortArray
    (get-1d [m x]
      (.get m (int x)))
    (get-2d [m x y]
      (.get m (int x) (int y)))
    (get-nd [m indices]
      (.get m (int-array indices))))

(extend-protocol mp/PIndexedAccess
  HDF5TimeDurationMDArray
    (get-1d [m x]
      (.get m (int x)))
    (get-2d [m x y]
      (.get m (int x) (int y)))
    (get-nd [m indices]
      (.get m (int-array indices)))
  MDByteArray
    (get-1d [m x]
      (.get m (int x)))
    (get-2d [m x y]
      (.get m (int x) (int y)))
    (get-nd [m indices]
      (.get m (int-array indices)))
  MDDoubleArray
    (get-1d [m x]
      (.get m (int x)))
    (get-2d [m x y]
      (.get m (int x) (int y)))
    (get-nd [m indices]
      (.get m (int-array indices)))
  MDFloatArray
    (get-1d [m x]
      (.get m (int x)))
    (get-2d [m x y]
      (.get m (int x) (int y)))
    (get-nd [m indices]
      (.get m (int-array indices)))
  MDIntArray
    (get-1d [m x]
      (.get m (int x)))
    (get-2d [m x y]
      (.get m (int x) (int y)))
    (get-nd [m indices]
      (.get m (int-array indices)))
  MDLongArray
    (get-1d [m x]
      (.get m (int x)))
    (get-2d [m x y]
      (.get m (int x) (int y)))
    (get-nd [m indices]
      (.get m (int-array indices)))
  MDShortArray
    (get-1d [m x]
      (.get m (int x)))
    (get-2d [m x y]
      (.get m (int x) (int y)))
    (get-nd [m indices]
      (.get m (int-array indices))))