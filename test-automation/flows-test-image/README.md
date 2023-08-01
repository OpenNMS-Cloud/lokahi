# snmpd-udpgen
It is an image for generate flow data while still receiving snmp data. However, it is for testing purpose. 
It is base on ehazlett/snmpd and https://github.com/OpenNMS/udpgen

# parameters
-s "all options will pass to snmpd"
-u "all options will pass to udpgen"

# Example docker-compose.yaml
```
---

version: '3.5'

services:
  snmpd-udpgen:
    image: snmpd-udpgen
    command: -s "-c /etc/snmp/snmpd.conf" -u "-h <SERVER_IP> -p 9996 -x netflow9 -r 1"
```
# docker command
docker run --rm snmpd-udpgen -s "-c /etc/snmp/snmpd.conf" -u "-h <SERVER_IP> -p 9996 -x netflow9 -r 1"
