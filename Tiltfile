# Tilt config #
secret_settings(disable_scrub=True)  ## TODO: update secret values so we can reenable scrub

# Functions #
cluster_arch_cmd = '$(tilt get cluster default -o=jsonpath --template="{.status.arch}")'

def jib_project(resource_name, image_name, base_path, k8s_resource_name, resource_deps=[], port_forwards=[], labels=None, submodule=None):
    if not labels:
        labels=[resource_name]

    submodule_path = ''
    submodule_flag = ''
    if submodule:
        submodule_path = '/{}'.format(submodule)
        submodule_flag = '-pl {}'.format(submodule)


    compile_resource_name = '{}-compile'.format(resource_name)

    local_resource(
        compile_resource_name,
        'mvn compile -f {} -am {}'.format(base_path, submodule_flag),
        deps=['{}/src'.format(base_path), '{}/pom.xml'.format(base_path)],
        ignore=['**/target'],
        labels=labels,
    )

    custom_build(
        image_name,
        'mvn jib:dockerBuild -Dimage=$EXPECTED_REF -f {} -Djib.from.platforms=linux/{} {}'.format(base_path, cluster_arch_cmd, submodule_flag),
        deps=['{}{}/target/classes'.format(base_path, submodule_path), '{}{}/pom.xml'.format(base_path, submodule_path), '{}{}/src/main/resources'.format(base_path, submodule_path)],
        live_update=[
            sync('{}{}/target/classes/org/opennms'.format(base_path, submodule_path), '/app/classes/org/opennms'),
            sync('{}{}/src/main/resources'.format(base_path, submodule_path), '/app/resources'),
        ],
    )

    k8s_resource(
        k8s_resource_name,
        new_name=resource_name,
        labels=labels,
        resource_deps=resource_deps + [compile_resource_name],
        port_forwards=port_forwards
    )

# Deployment #
k8s_yaml(
    helm(
        'charts/opennms',
        values=['./tilt-helm-values.yaml'],
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

### Inventory ###
jib_project(
    'inventory',
    'opennms/horizon-stream-inventory',
    'inventory',
    'opennms-inventory',
    port_forwards=['29080:9090', '29050:5005', '29065:6565'],
)

### Metrics Processor ###
jib_project(
    'metrics-processor',
    'opennms/horizon-stream-metrics-processor',
    'metrics-processor',
    'opennms-metrics-processor',
    port_forwards=['28080:9090', '28050:5005'],
)

### Events ###
jib_project(
    'events',
    'opennms/horizon-stream-events',
    'events',
    'opennms-events',
    port_forwards=['30050:5005'],
)

### Minion Gateway ###
jib_project(
    'minion-gateway',
    'opennms/horizon-stream-minion-gateway',
    'minion-gateway',
    'opennms-minion-gateway',
    submodule='main',
    port_forwards=['16080:9090', '16050:5005'],
)

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

### Minion ###
custom_build(
    'opennms/horizon-stream-minion',
    ['mvn', 'install', '-Ddocker.image=$EXPECTED_REF', '-Dtest=false', '-DfailIfNoTests=false', '-DskipITs=true', '-DskipUTs=true', '-DskipTests=true', '-Dfeatures.verify.skip=true'],
    deps=['./minion'],
    ignore=['**/target', '**/dependency-reduced-pom.xml'],
)

k8s_resource(
    'opennms-minion',
    new_name='minion-k8s',
    port_forwards=['12022:8101', '12080:8181', '12050:5005'],
    labels=['minion'],
    trigger_mode=TRIGGER_MODE_MANUAL,
)

local_resource(
    'minion-local',
    cmd=['mvn', 'install', '-Ddocker.skip=true', '-Dtest=false', '-DfailIfNoTests=false', '-DskipITs=true', '-DskipUTs=true', '-DskipTests=true', '-Dfeatures.verify.skip=true'],
    dir='minion',
    serve_cmd=['./bin/karaf', 'server'],
    serve_dir='minion/assembly/target/assembly',
    serve_env={
        "USE_KUBERNETES": "false",
        "LOCATION": "Default"
    },
    labels=['minion'],
    trigger_mode=TRIGGER_MODE_MANUAL,
    allow_parallel=True,
    auto_init=False
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
    new_name='keycloak',
    port_forwards=['26080:8080'],
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

### Prometheus ###
k8s_resource(
    'prometheus',
    port_forwards=['19080:9090'],
)

### Prometheus Push Gateway ###
k8s_resource(
    'prometheus-pushgateway',
    port_forwards=['21080:9091'],
)

### Others ###
k8s_resource(
    'ingress-nginx-controller',
    port_forwards=['8123:80'],
)
