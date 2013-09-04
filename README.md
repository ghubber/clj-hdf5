Overview:
=========
This library provides a high-level Clojure interface to the HDF5
library for storing scientific data. It is built on top of the JHDF5
library, which provides a high-level Java interface. While JHDF5 can
be used directly from Clojure, this additional layer adds a lot of
convenience:

- Uses Clojure vectors rather than Java arrays for array I/O.
- Multimethods reduce the huge Java API to just a few functions.

This library is work in progress. Only scalar and 1D array data
are supported at the moment, both for datasets and attributes.


Building:
=========
This build depends on a local jar for JHDF5. Build is managed by Leiningen, and the local jar dependency is resolved by creating a local maven repository more or less like:

    mvn deploy:deploy-file -DgroupId=cisd -DartifactId=jhdf5 \
    -Dversion=13.06.2 -Dpackaging=jar \
    -Dfile=/path/to/cisd-jhdf5-batteries_included_lin_win_mac_sol.jar \
    -Durl=file:repo


Building Documentation:
=======================
API documentation can be rebuilt using leiningen and codox:

   lein doc


Basic Usage:
============
    ;; Create and open a new HDF5 file / storage container
    (def myh5root
         (create (clojure.java.io/file "/path/to/file.h5")))
    
    ;; Open an existing HDF5 file (read-only)
    (def myh5root
         (open (clojure.java.io/file "/path/to/file.h5")))

    ;; Open an existing HDF5 file (read-write)
    (def myh5root
         (open (clojure.java.io/file "/path/to/file.h5" :read-write)))

    ;; Close a file
    (close myh5root)

    ;; Create a group in an HDF5 file
    (create-group myh5root "examplegroup")

    ;; Create an attribute of the HDF5 root /
    (create-attribute myh5root "myattribute" "some value")

    ;; ... more
    


Links:
======

HDF5
----
http://www.hdfgroup.org/

JHDF5
-----
https://wiki-bsse.ethz.ch/display/JHDF5/JHDF5+(HDF5+for+Java)
