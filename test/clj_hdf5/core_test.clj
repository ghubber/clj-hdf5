(ns clj-hdf5.core-test
  (:require [clojure.java.io :as io])
  (:use [clojure.test]
        [clj-hdf5.core]))

(def tmp-dir (System/getProperty "java.io.tmpdir"))



(use-fixtures :each
              (fn [f]
                (if (.exists (io/file tmp-dir "file.h5"))
                  (io/delete-file (io/file tmp-dir "file.h5")))
                (f)
                (io/delete-file (io/file tmp-dir "file.h5"))))


(deftest create-file-with-read-write
  (testing "create a file with open"
    (let [h5file (io/file tmp-dir "file.h5")
          h5root (open h5file :read-write)]
      (.exists h5file))))



