#ARG GRAFANA_VERSION="8.2.3"
ARG GRAFANA_VERSION="10.0.10"
FROM grafana/grafana:${GRAFANA_VERSION}

ARG GF_INSTALL_PLUGINS="https://github.com/OpenNMS/opennms-helm/releases/download/v7.2.0/opennms-helm-app-7.2.0.zip;opennms-helm-app"
#ARG GF_INSTALL_PLUGINS="https://github.com/OpenNMS/opennms-helm/releases/download/v7.1.2/opennms-helm-app-7.1.2.zip;opennms-helm-app"
ENV GF_PLUGINS_ALLOW_LOADING_UNSIGNED_PLUGINS="opennms-helm-app"

USER root

RUN apk add --update --no-cache libcap

ARG GF_GID="0"
ARG GF_INSTALL_IMAGE_RENDERER_PLUGIN="false"
ENV GF_PATHS_PLUGINS="/var/lib/grafana-plugins"

RUN mkdir -p "$GF_PATHS_PLUGINS" && \
    chown -R grafana:${GF_GID} "$GF_PATHS_PLUGINS"

RUN if [ $GF_INSTALL_IMAGE_RENDERER_PLUGIN = "true" ]; then \
    echo "http://dl-cdn.alpinelinux.org/alpine/edge/community" >> /etc/apk/repositories && \
    echo "http://dl-cdn.alpinelinux.org/alpine/edge/main" >> /etc/apk/repositories && \
    echo "http://dl-cdn.alpinelinux.org/alpine/edge/testing" >> /etc/apk/repositories && \
    apk --no-cache  upgrade && \
    apk add --no-cache udev ttf-opensans chromium && \
    rm -rf /tmp/* && \
    rm -rf /usr/share/grafana/tools/phantomjs; \
fi

USER grafana

ENV GF_PLUGIN_RENDERING_CHROME_BIN="/usr/bin/chromium-browser"

RUN if [ $GF_INSTALL_IMAGE_RENDERER_PLUGIN = "true" ]; then \
    grafana-cli \
        --pluginsDir "$GF_PATHS_PLUGINS" \
        --pluginUrl https://github.com/grafana/grafana-image-renderer/releases/latest/download/plugin-linux-x64-glibc-no-chromium.zip \
        plugins install grafana-image-renderer; \
fi

RUN if [ ! -z "${GF_INSTALL_PLUGINS}" ]; then \
    OLDIFS=$IFS; \
    IFS=','; \
    echo "test 1; " >> $GF_PATHS_PLUGINS/tmp.del; \
    for plugin in ${GF_INSTALL_PLUGINS}; do \
        echo "test 2; " >> $GF_PATHS_PLUGINS/tmp.del; \
        IFS=$OLDIFS; \
        if expr match "$plugin" '.*\;.*'; then \
            echo "test 3; " >> $GF_PATHS_PLUGINS/tmp.del; \
            pluginUrl=$(echo "$plugin" | cut -d';' -f 1); \
            pluginInstallFolder=$(echo "$plugin" | cut -d';' -f 2); \
            grafana-cli --pluginUrl ${pluginUrl} --pluginsDir "${GF_PATHS_PLUGINS}" plugins install "${pluginInstallFolder}"; \
        else \
            echo "test 4; " >> $GF_PATHS_PLUGINS/tmp.del; \
            grafana-cli --pluginsDir "${GF_PATHS_PLUGINS}" plugins install ${plugin}; \
        fi \
    done \
fi

CMD echo "This is a test"

ENTRYPOINT [ "/run.sh" ]
