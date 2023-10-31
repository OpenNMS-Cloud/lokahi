# syntax=docker/dockerfile:1

##
## RELEASE IMAGE
##

ARG BASE_IMAGE="eclipse-temurin:17"

FROM ${BASE_IMAGE} as tarball

RUN mkdir -p /opt

ADD --chown=10001:0 work/karaf/ /opt/karaf/
ADD --chown=10001:0 work/entrypoint/ /
ADD --chown=10001:0 work/agent/ /opt/karaf/agent/

# Set karaf.clean.cache = true in system.properties
RUN sed -i 's/^karaf.clean.cache *=.*/karaf.clean.cache = true/' /opt/karaf/etc/system.properties

# Allow users in the root group to access them with the same authorization as the directory and file owner
# This will double the size of the our code layer because we have two copies of the karaf files -- before and after the chmod.
# To avoid this, we create a 'tarball' image, do the chmod in there, and then use that to create the final image.
RUN chmod -R g=u /opt/karaf/

FROM ${BASE_IMAGE}

RUN apt-get update && \
    apt-get \
        --no-install-recommends \
        -yq \
        install \
            jq unzip

RUN groupadd --gid 10001 minion && \
    useradd --system --uid 10001 --gid minion minion --home-dir /opt/karaf

RUN mkdir -p /opt
COPY --chown=10001:0 --from=tarball /opt/karaf /opt/karaf
COPY --chown=10001:0 --from=tarball /entrypoint.sh /entrypoint.sh

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

RUN echo "org.opencontainers.image.created=${BUILD_DATE}" >> /opt/karaf/etc/org.opennms.version.cfg
RUN echo "org.opencontainers.image.version=${VERSION}" >> /opt/karaf/etc/org.opennms.version.cfg

WORKDIR /opt/karaf

USER 10001

ENTRYPOINT [ "/entrypoint.sh" ]

# Use CMD to pass the "daemon" command-line argument to bin/karaf; this way, it can easily be changed to
#  other arguments such as "debug" without having to remember the path to the entrypoint script.
CMD [ "daemon" ]

STOPSIGNAL SIGTERM

### Runtime information and not relevant at build time
ENV IGNITE_UPDATE_NOTIFIER="false"

##------------------------------------------------------------------------------
## EXPOSED PORTS
##------------------------------------------------------------------------------
## -- Java Debug - JDWP    5005/TCP
## -- Karaf SSH            8101/TCP
## -- SNMP Trapd           1162/UDP
## -- Flows                9999/UDP
EXPOSE 5005/tcp 8101/tcp 1162/udp 9999/udp
