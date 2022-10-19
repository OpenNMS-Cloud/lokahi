# Tilt config #
load('ext://tilt_inspector', 'tilt_inspector')
tilt_inspector()

secret_settings(disable_scrub=True)  ## TODO: update secret values so we can reenable scrub

cluster_arch_cmd = '$(tilt get cluster default -o=jsonpath --template="{.status.arch}")'

# Deployment #
k8s_yaml(
    helm(
        'charts/opennms',
        values=['./skaffold-helm-values.yaml'],
    )
)

# Builds #
## Shared ##
local_resource(
    'parent-pom',
    cmd='mvn clean install -N',
    dir='parent-pom',
    deps=['./parent-pom'],
    ignore=['**/target'],
    labels=['opennms'],
)

local_resource(
    'shared-lib',
    cmd='mvn clean install -DskipTests=true',
    dir='shared-lib',
    deps=['./shared-lib'],
    ignore=['**/target'],
    labels=['opennms'],
)

## OpenNMS ##
### Core ###
custom_build(
    'opennms/horizon-stream-core',
    'mvn install -Pbuild-docker-images-enabled -DskipTests -Ddocker.image=$EXPECTED_REF -f platform',
    deps=['./platform'],
    ignore=['**/target', '**/dependency-reduced-pom.xml'],
)

k8s_resource(
    'opennms-core',
    port_forwards=['11022:8101', '11080:8181', '11050:5005'],
    labels=['opennms'],
)

### Notification ###
local_resource(
    'compile-notifications',
    'mvn clean compile -f notifications',
    deps=['./notifications/src', './notifications/pom.xml'],
    ignore=['**/target'],
    labels=['opennms'],
)

custom_build(
    'opennms/horizon-stream-notification',
    'mvn jib:dockerBuild -Dimage=$EXPECTED_REF -f notifications -Djib.from.platforms=linux/' + cluster_arch_cmd,
    deps=['./notifications/target/classes', './notifications/pom.xml'],
    live_update=[
        sync('./notifications/target/classes', '/app/classes'),
    ],
)

k8s_resource(
    'opennms-notifications',
    port_forwards=['15080:8080', '15050:5005'],
    labels=['opennms'],
    resource_deps=['compile-notifications'],
)

### Vue.js BFF ###
local_resource(
    'compile-rest-server',
    'mvn clean compile -f rest-server',
    deps=['./rest-server/src', './rest-server/pom.xml'],
    ignore=['**/target'],
    labels=['opennms'],
)

custom_build(
    'opennms/horizon-stream-rest-server',
    'mvn jib:dockerBuild -Dimage=$EXPECTED_REF -f rest-server -Djib.from.platforms=linux/' + cluster_arch_cmd,
    deps=['./rest-server/target/classes', './rest-server/pom.xml'],
    live_update=[
        sync('./rest-server/target/classes', '/app/classes'),
    ],
)

k8s_resource(
    'opennms-rest-server',
    port_forwards=['13080:9090', '13050:5005'],
    labels=['opennms'],
    resource_deps=['compile-rest-server'],
)

### Vue.js UI ###
docker_build(
    'opennms/horizon-stream-ui',
    'ui',
    target='development',
    live_update=[
        sync('./ui', '/app'),
        run('yarn install', trigger=['./ui/package.json', './ui/yarn.lock']),
    ],
)
k8s_resource(
    'opennms-ui',
    port_forwards=['17080:80'],
    labels=['opennms'],
)

### Minion Gateway ###
local_resource(
    'compile-minion-gateway',
    'mvn clean compile -f minion-gateway -pl main -am',
    deps=['./minion-gateway/main/src', './minion-gateway/main/pom.xml'],
    ignore=['**/target'],
    labels=['opennms'],
)

custom_build(
    'opennms/horizon-stream-minion-gateway',
    'mvn jib:dockerBuild -Dimage=$EXPECTED_REF -f minion-gateway -pl main -Djib.from.platforms=linux/' + cluster_arch_cmd,
    deps=['./minion-gateway/main/target/classes', './minion-gateway/main/pom.xml'],
    live_update=[
        sync('./minion-gateway/main/target/classes', '/app/classes'),
    ],
)

k8s_resource(
    'opennms-minion-gateway',
    port_forwards=['16080:8080', '16089:8990'],
    labels=['opennms'],
    resource_deps=['compile-minion-gateway'],
)

### Minion ###
custom_build(
    'opennms/horizon-stream-minion',
    'mvn install -f minion -Ddocker.image=$EXPECTED_REF -Dtest=false -DfailIfNoTests=false -DskipITs=true -DskipTests=true',
    deps=['./minion'],
    ignore=['**/target', '**/dependency-reduced-pom.xml'],
)

k8s_resource(
    'opennms-minion',
    port_forwards=['12022:8101', '12080:8181', '12050:5005'],
    labels=['opennms'],
)

## 3rd Party Resources ##
### Keycloak ###
docker_build(
    'opennms/horizon-stream-keycloak',
    'keycloak-ui',
    target='development',
    live_update=[
        sync('./keycloak-ui/themes', '/opt/keycloak/themes')
    ],
)
k8s_resource(
    'onms-keycloak',
)

### Grafana ###
docker_build(
    'opennms/horizon-stream-grafana',
    'grafana',
)
k8s_resource(
    'grafana',
    port_forwards=['18080:3000'],
)

### Others ###
k8s_resource(
    'ingress-nginx-controller',
    port_forwards=['8123:80'],
)
