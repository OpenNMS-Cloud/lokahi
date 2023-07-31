FROM opennms/udpgen:latest AS udpgen-build

WORKDIR /

FROM ehazlett/snmpd 
RUN apt install -y tmux

# Copy udpgen from udpgen-build above to current image stage.
COPY --from=udpgen-build /udpgen /udpgen 

ADD bootstrap.sh /bootstrap.sh
CMD []
ENTRYPOINT ["/bootstrap.sh"]
