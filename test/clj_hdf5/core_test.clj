(ns clj-hdf5.core-test
  (:require [clojure.java.io :as io])
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


(deftest create-a-new-group
  (testing "Creating a new group"))






