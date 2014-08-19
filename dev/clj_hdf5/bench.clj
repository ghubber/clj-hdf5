(ns clj-hdf5.bench
  (:require [criterium.core :as cc])
  (:use clojure.core.matrix)
  (:require [clojure.core.matrix.compliance-tester :as ct])
  (:require [clojure.core.matrix.protocols :as mp])
  (:require [clj-hdf5.mdarray-abstract :as a])
  (:require [clj-hdf5.mdarray :as c]))

(defn get-concrete-mdarrays []
  [(c/empty-mdarray-double [1 2 3 4 5 6])
   (c/empty-mdarray-float [1 2 3 4 5 6])
   (c/empty-mdarray-int [1 2 3 4 5 6])
   (c/empty-mdarray-long [1 2 3 4 5 6])
   (c/empty-mdarray-short [1 2 3 4 5 6])])

(defn get-abstract-mdarrays []
      [(a/empty-mdarray-double [1 2 3 4 5 6])
       (a/empty-mdarray-float [1 2 3 4 5 6])
       (a/empty-mdarray-int [1 2 3 4 5 6])
       (a/empty-mdarray-long [1 2 3 4 5 6])
       (a/empty-mdarray-short [1 2 3 4 5 6])])

(defn benchmark-concrete []
  (cc/quick-bench (get-concrete-mdarrays )))

(defn benchmark-abstract []
      (cc/quick-bench (get-abstract-mdarrays )))