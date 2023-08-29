FROM opennms/udpgen:latest

WORKDIR /

# Add a basic SNMP Daemon to the image
RUN apt update && apt install -y snmpd
RUN sed -i '/^agentaddress.*/c\agentaddress  udp:161' /etc/snmp/snmpd.conf

ADD bootstrap.sh /bootstrap.sh
CMD []
ENTRYPOINT ["/bootstrap.sh"]

