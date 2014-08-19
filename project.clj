(defproject clj-hdf5 "0.2.1"
  :description "HDF5 interface for Clojure based on JHDF5"
  :url "https://github.com/clojure-numerics/clj-hdf5"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :aot [clj-hdf5.core]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [net.mikera/core.matrix "0.28.0"]
                 [io.kimchi/cisd-jhdf5-core "13.06.2"]
                 [io.kimchi/cisd-jhdf5-native-deps "13.06.2"]]
  :profiles {:dev {:arch :x86_64
                   :source-paths ["dev"]
                   :dependencies [[criterium "0.4.3"]]}}
  :plugins [[codox "0.6.4"]])
