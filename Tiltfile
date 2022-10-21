# Tilt config #
load('ext://tilt_inspector', 'tilt_inspector')
tilt_inspector()

secret_settings(disable_scrub=True)  ## TODO: update secret values so we can reenable scrub

load('tilt/Tiltfile-helpers', 'jib_project', 'inject_java_debug')

# Deployment #
decoded = decode_yaml_stream(helm(
    'charts/opennms',
    values=['./skaffold-helm-values.yaml'],
))

# Builds #
## Shared ##
local_resource(
    'parent-pom',
    cmd='mvn clean install -N',
    dir='parent-pom',
    deps=['./parent-pom'],
    ignore=['**/target'],
    labels=['shared'],
)

local_resource(
    'shared-lib',
    cmd='mvn clean install -DskipTests=true',
    dir='shared-lib',
    deps=['./shared-lib'],
    ignore=['**/target'],
    labels=['shared'],
    resource_deps=['parent-pom'],
)

## Microservices ##
### Notification ###
jib_project(
    'notification',
    'opennms/horizon-stream-notification',
    'notifications',
    'opennms-notifications',
    port_forwards=['15080:9090', '15050:5005'],
)
inject_java_debug(decoded, 'opennms-notifications', 'opennms-notifications',)

### Vue.js App ###
#### UI ####
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
    new_name='vuejs-ui',
    port_forwards=['17080:80'],
    labels=['vuejs-app'],
)

#### BFF ####
jib_project(
    'vuejs-bff',
    'opennms/horizon-stream-rest-server',
    'rest-server',
    'opennms-rest-server',
    labels=['vuejs-app'],
    port_forwards=['13080:9090', '13050:5005'],
)
inject_java_debug(decoded, 'opennms-rest-server', 'opennms-rest-server',)

### Inventory ###
jib_project(
    'inventory',
    'opennms/horizon-stream-inventory',
    'inventory',
    'opennms-inventory',
    port_forwards=['29080:9090', '29050:5005'],
)
inject_java_debug(decoded, 'opennms-inventory', 'opennms-inventory',)

### Metrics Processor ###
jib_project(
    'metrics-processor',
    'opennms/horizon-stream-metrics-processor',
    'metrics-processor',
    'opennms-metrics-processor',
    port_forwards=['30080:9090', '30050:5005'],
)
inject_java_debug(decoded, 'opennms-metrics-processor', 'opennms-metrics-processor',)

### Minion Gateway ###
jib_project(
    'minion-gateway',
    'opennms/horizon-stream-minion-gateway',
    'minion-gateway',
    'opennms-minion-gateway',
    submodule='main',
    port_forwards=['16080:9090', '16050:5005'],
)
inject_java_debug(decoded, 'opennms-minion-gateway', 'opennms-minion-gateway',)

### Core ###
custom_build(
    'opennms/horizon-stream-core',
    'mvn install -Pbuild-docker-images-enabled -DskipTests -Ddocker.image=$EXPECTED_REF -f platform',
    deps=['./platform'],
    ignore=['**/target', '**/dependency-reduced-pom.xml'],
)

k8s_resource(
    'opennms-core',
    new_name='core',
    port_forwards=['11022:8101', '11080:8181', '11050:5005'],
    labels=['core'],
    trigger_mode=TRIGGER_MODE_MANUAL,
)

inject_java_debug(decoded, 'opennms-core', 'opennms-core',)

### Minion ###
custom_build(
    'opennms/horizon-stream-minion',
    'mvn install -f minion -Ddocker.image=$EXPECTED_REF -Dtest=false -DfailIfNoTests=false -DskipITs=true -DskipTests=true',
    deps=['./minion'],
    ignore=['**/target', '**/dependency-reduced-pom.xml'],
)

k8s_resource(
    'opennms-minion',
    new_name='minion',
    port_forwards=['12022:8101', '12080:8181', '12050:5005'],
    labels=['minion'],
    trigger_mode=TRIGGER_MODE_MANUAL,
)

inject_java_debug(decoded, 'opennms-minion', 'opennms-minion',)

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
    new_name='keycloak',
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

# Deploy #
k8s_yaml(encode_yaml_stream(decoded))
