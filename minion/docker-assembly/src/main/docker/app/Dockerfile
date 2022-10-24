##
## RELEASE IMAGE
##
FROM opennms/deploy-base:jre-1.2.0.b105

RUN mkdir -p /opt

RUN groupadd --gid 10001 minion-poc && \
    useradd --system --uid 10001 --gid minion-poc minion-poc --home-dir /opt/karaf

ADD --chown=10001:0 work/karaf/ /opt/karaf/

# Set karaf.clean.cache = true in system.properties
RUN sed -i 's/^karaf.clean.cache *=.*/karaf.clean.cache = true/' /opt/karaf/etc/system.properties

# Create SSH Key-Pair to use with the Karaf Shell
# This is a workaround to be able to use our health:check which does not work with the karaf/bin/client command
RUN mkdir /opt/karaf/.ssh && \
    ssh-keygen -t rsa -f /opt/karaf/.ssh/id_rsa -q -N "" && \
    chmod 700 /opt/karaf/.ssh && \
    chmod 600 /opt/karaf/.ssh/id_rsa && \
    chown 10001 /opt/karaf/.ssh -R && \
    echo minion=$(cat /opt/karaf/.ssh/id_rsa.pub | awk '{print $2}'),viewer > /opt/karaf/etc/keys.properties && \
    echo "_g_\\:admingroup = group,admin,manager,viewer,systembundles,ssh" >> /opt/karaf/etc/keys.properties

# Arguments for labels should not invalidate caches
ARG BUILD_DATE="1970-01-01T00:00:00+0000"
ARG VERSION
ARG SOURCE
ARG REVISION
ARG BUILD_JOB_ID
ARG BUILD_NUMBER
ARG BUILD_URL
ARG BUILD_BRANCH

LABEL org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.title="TBD ${VERSION}" \
      org.opencontainers.image.source="${SOURCE}" \
      org.opencontainers.image.revision="${REVISION}" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.vendor="The OpenNMS Group, Inc." \
      org.opencontainers.image.authors="OpenNMS Community" \
      org.opencontainers.image.licenses="AGPL-3.0" \
      org.opennms.image.base="${BASE_IMAGE}" \
      org.opennme.cicd.jobid="${BUILD_JOB_ID}" \
      org.opennms.cicd.buildnumber="${BUILD_NUMBER}" \
      org.opennms.cicd.buildurl="${BUILD_URL}" \
      org.opennms.cicd.branch="${BUILD_BRANCH}"

WORKDIR /opt/karaf

USER 10001

ENV KARAF_SYSTEM_OPTS "--add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.lang.invoke=ALL-UNNAMED"

ENTRYPOINT [ "/opt/karaf/bin/karaf" ]

# Use CMD to pass the "daemon" command-line argument to bin/karaf; this way, it can easily be changed to
#  other arguments such as "debug" without having to remember the path to the entrypoint script.
CMD [ "daemon" ]

STOPSIGNAL SIGTERM

### Runtime information and not relevant at build time
ENV MINION_ID="00000000-0000-0000-0000-deadbeef0001" \
    MINION_LOCATION="MINION" \
    OPENNMS_BROKER_URL="tcp://127.0.0.1:61616" \
    OPENNMS_HTTP_URL="http://127.0.0.1:8980/opennms" \
    OPENNMS_HTTP_USER="minion" \
    OPENNMS_HTTP_PASS="minion" \
    OPENNMS_BROKER_USER="minion" \
    OPENNMS_BROKER_PASS="minion"

##------------------------------------------------------------------------------
## EXPOSED PORTS
##------------------------------------------------------------------------------
## -- Java Debug - JDWP    5005/TCP
## -- Karaf SSH            8101/TCP
## -- HTTP                 8181/TCP
## -- SNMP Trapd           1162/UDP
## -- Syslog               1514/UDP
EXPOSE 5005/tcp 8101/tcp 8181/tcp 1162/udp 1514/udp
