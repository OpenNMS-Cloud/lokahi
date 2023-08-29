# snmpd-udpgen
This is an image that supports SNMP discover and contains the tooling to generate flow data.
It is for testing purposes and is based on https://github.com/OpenNMS/udpgen
Workflow published the image as opennms/lokahi-snmpd-udpgen:latest.

# parameters
All parameters are passed to snmpd. udpgen must be run by separately executing into the container
as needed.

# Example docker-compose.yaml
```
---

version: '3.5'

services:
  snmpd-udpgen:
    image: snmpd-udpgen
    command: -c /etc/snmp/snmpd.conf
```
# docker command
docker run --rm snmpd-udpgen
