(defproject clj-hdf5 "0.2"
  :description "HDF5 interface for Clojure based on JHDF5"
  :url "https://github.com/clojure-numerics/clj-hdf5"
  :aot [clj-hdf5.core]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [net.mikera/core.matrix "0.3.0"]
                 [cisd/jhdf5 "12.02.3"]
		 ;Figure out what is needed from old contrib
                 ;[org.clojure/clojure-contrib "1.2.0"]
                 ]
  :repositories {"project" "file:repo"}
  :plugins [[codox "0.6.4"]])
