## Running Minion from command line in locally

```
# optional: set host for gateway, eg. when using wire mock use 'localhost'
nano ./assembly/target/assembly/etc/org.opennms.core.ipc.grpc.client.cfg

# optional: enable karaf user for ssh access: uncomment karaf user: 
nano ./assembly/target/assembly/etc/users.properties

# run minion:
./assembly/target/assembly/bin/karaf debug


```

