##
# Pre-stage image to extract and manipulate Minion directory structure
# Normally we install to /opt/horizon-stream and not /opt/horizon-stream-XX.X.X-SNAPSHOT
# To avoid issues, we rearrange the directories in pre-stage to avoid injecting these
# as additional layers into the final image.
##
ARG BASE_IMAGE="opennms/deploy-base:jre-1.2.0.b105"

FROM ${BASE_IMAGE} as minion-base

ADD work/core-dynamic-dist.tar.gz /opt/

# Organize files based on the build home dir /opt/horizon-stream
RUN mv /opt/core-dynamic-dist-* /opt/horizon-stream

# We set the correct permissions in this stage, so we don't have this as an additional
# layer in our deploy image.
RUN groupadd --gid 10001 horizon-stream && \
    useradd --system --uid 10001 --gid horizon-stream horizon-stream --home-dir /opt/horizon-stream && \
    chown 10001 /opt/horizon-stream -R && \
    chgrp -R 0 /opt/horizon-stream && \
    chmod -R g=u /opt/horizon-stream

# Set karaf.clean.cache = true in system.properties
RUN sed -i 's/^karaf.clean.cache *=.*/karaf.clean.cache = true/' /opt/horizon-stream/etc/system.properties

##
# Prod image with minimal image size
##
FROM ${BASE_IMAGE}

# Create user to be able to run as non root with a known user and group id.
RUN groupadd --gid 10001 horizon-stream && \
    useradd --system --uid 10001 --gid horizon-stream horizon-stream --home-dir /opt/horizon-stream

# Install entrypoint wrapper and health check script
COPY container-fs/entrypoint.sh /
COPY container-fs/health.sh /

# If you copy from /opt/horizon-stream to /opt/horizon-stream the permissions are not preserved
# We would have 755 for minion:root instead of 775 and prevents writing lock files in /opt/horizon-stream
COPY --chown=10001:0 --from=minion-base /opt /opt

# Copy Prometheus JMX Exporter
COPY --chown=10001:0 --from=minion-base /opt/prom-jmx-exporter /opt/prom-jmx-exporter

# Create the directory for server certificates
RUN mkdir /opt/horizon-stream/server-certs

# Create SSH Key-Pair to use with the Karaf Shell
# This is a workaround to be able to use our health:check which does not work with the karaf/bin/client command
RUN mkdir /opt/horizon-stream/.ssh && \
    ssh-keygen -t rsa -f /opt/horizon-stream/.ssh/id_rsa -q -N "" && \
    chmod 700 /opt/horizon-stream/.ssh && \
    chmod 600 /opt/horizon-stream/.ssh/id_rsa && \
    chown 10001 /opt/horizon-stream/.ssh -R && \
    echo minion=$(cat /opt/horizon-stream/.ssh/id_rsa.pub | awk '{print $2}'),viewer > /opt/horizon-stream/etc/keys.properties && \
    echo "_g_\\:admingroup = group,admin,manager,viewer,systembundles,ssh" >> /opt/horizon-stream/etc/keys.properties

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
      org.opencontainers.image.title="Horizon Stream Core ${VERSION}" \
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

WORKDIR /opt/horizon-stream

USER 10001

ENTRYPOINT [ "/entrypoint.sh" ]

STOPSIGNAL SIGTERM

CMD [ "-f" ]

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
