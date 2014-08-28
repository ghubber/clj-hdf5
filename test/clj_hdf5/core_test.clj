(ns clj-hdf5.core-test
  (:require [clojure.java.io :as io])
  (:require [clojure.core.matrix :as m])
  (:use [clojure.test]
        [clj-hdf5.core]))

(def tmp-dir (System/getProperty "java.io.tmpdir"))

(defn get-tmp-file []
  (io/file tmp-dir "file.h5"))

(defmacro with-open
  [bindings & body]
  `(let [~(first bindings) (open ~(second bindings) ~(bindings 2))]
      (try
        ~@body
        (finally
          (close ~(first bindings))))))

(defn delete-tmp-file []
  (let [tmp-file get-tmp-file]
  (if (.exists (tmp-file))
    (io/delete-file (tmp-file)))))

(defn setup-each []
  (delete-tmp-file))
 
(defn teardown-each []
  (delete-tmp-file))

(defn fixture-each [f]
  (setup-each)
  (f)
  (teardown-each))

(use-fixtures :each fixture-each)

(deftest create-new-file-with-read-write
  (testing "Opening a file with :read-write should create the 
           file if it doesn't exist yet."
    (let [h5file (get-tmp-file)]
      (is (false? (.exists h5file)))
          (with-open [h5root h5file :read-write]
            (is (true? (.exists h5file)))
            (is (true? (root? h5root)))))))

(deftest create-nested-groups
  (testing "Creating a new group")
  (let [h5file (get-tmp-file)]
    (with-open [h5root h5file :read-write]
      (let [nested-group (create-group h5root "node1/node2/node3")
            node1 (lookup h5root "node1")
            node2 (lookup node1 "node2")
            node3 (lookup node2 "node3")]
        (is (= "/node1" (:path node1)))
        (is (= "/node1/node2" (:path node2)))
        (is (= "/node1/node2/node3" (:path node3)))))))

(deftest create-datasets
(testing "Creation of datasets")
(let [h5file (get-tmp-file)]
  (with-open [root h5file :read-write]
             (let [ds1 (create-dataset root "ds1" [10] Float/TYPE)
                   ds2 (create-dataset root "ds2" [10 20] Float/TYPE)
                   ds3 (create-dataset root "ds3" [10 20 30] Float/TYPE)
                   ds4 (create-dataset root "ds4" [10 20 30 40] Float/TYPE)
                   data1 (read ds1)
                   data2 (read ds2)
                   data3 (read ds3)
                   data4 (read ds4)]
               (is (true? (dataset? ds1)))
               (is (true? (dataset? ds2)))
               (is (true? (dataset? ds3)))
               (is (true? (dataset? ds4)))
               (is (= 1 (m/dimensionality data1)))
               (is (= 2 (m/dimensionality data2)))
               (is (= 3 (m/dimensionality data3)))
               (is (= 4 (m/dimensionality data4)))))))





