# Quick Start

**NOTE:** All commands **must** be run from the project root


    $ ./build-tools/basic/compile.sh
    $ ./build-tools/basic/mk-docker-images.sh
    $ ./build-tools/basic/push-images-to-kind.sh
    $ ./build-tools/basic/run-cluster.sh
    $ ./build-tools/basic/run-port-forwards.sh


# Destroy the Cluster

* Only destroys project resources.  Does not remove the K8S cluster itself.


    $ ./build-tools/basic/destroy-cluster.sh

# Design Philosophy

* Scripts are extremely simple
* Developers can easily see how the entire process works
* Developers can easily copy-and-paste individual commands to streamline updates
* No logic, so these scripts just "do what they are told"
* Once a functional cluster is up-and-running, updating individual components,
for a faster development cycle, can be achieved easily
* Developer can be confident that updates they need/request are made
