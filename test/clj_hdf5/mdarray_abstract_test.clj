(ns clj-hdf5.mdarray-abstract-test
  (:use clojure.test)
  (:use clojure.core.matrix)
  (:require [clojure.core.matrix.compliance-tester :as ct])
  (:require [clojure.core.matrix.protocols :as mp])
  (:use clj-hdf5.mdarray-abstract))

(defn get-primitive-mdarrays []
  [(empty-mdarray-double [3 3])
   (empty-mdarray-float [3 3])
   (empty-mdarray-int [3 3])
   (empty-mdarray-long [3 3])
   (empty-mdarray-short [3 3])])

(deftest compliance-test-primitives
  (doseq [m (get-primitive-mdarrays)]
    (ct/compliance-test m)))