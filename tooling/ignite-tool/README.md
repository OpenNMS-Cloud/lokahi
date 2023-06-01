# Ignite Tool

Tool which joins an ignite cluster to provide developers, and possibly other users,
access to statistics, functionality, and more.

# To use with Kubernetes + Kind

    $ mvn clean install jib:dockerBuild -Dapplication.docker.image=opennms-ignite-tool:local
    $ kind load docker-image opennms-ignite-tool:local
    $ kubectl run opennms-ignite-tool --env ignite_use_kubernetes=true --env ignite_kubernetes_service_name=lokahi-ignite-core --restart=Never --rm -it --image opennms-ignite-tool:local --port 8080
    $ kubectl port-forward pod/opennms-ignite-tool 8008:8080
    $ cd tools
    $ ./publish-task-set
    $ ./publish-task-set example-task-set.002.json
    $ curl -s http://localhost:8008/ignite/topology/latest
    $ curl -s http://localhost:8008/ignite/topology/1
