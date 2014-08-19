(ns clj-hdf5.mdarray-test
  (:use clojure.test)
  (:use clojure.core.matrix)
  (:require [clojure.core.matrix.compliance-tester :as ct])
  (:require [clojure.core.matrix.protocols :as mp])
  (:use clj-hdf5.mdarray))

(deftest compliance-test
  (ct/compliance-test (empty-mdarray-double [3 3])))