## LOCAL PORT ASSIGNMENTS (note internal ports use the native port number)
##
##      xx = service/container
##      xx080 = HTTP
##      xx089 = GRPC
##      xx022 = SSH
##      xx025 = SMTP (mail)
##      xx050 = JAVA DEBUG
##      xx054 = POSTGRES
##      xx090 = KAFKA
##
##	Exceptions to the Rule
##		8123 = INGRESS HTTP
##
## 11 = lokahi-core
## 12 = lokahi-minion
## 13 = lokahi-api
## 14 = api-gateway
## 15 = lokahi-notification
## 16 = lokahi-minion-gateway
## 17 = lokahi-ui
## 18 = grafana
## 22 = mail-server
## 23 = zookeeper
## 24 = kafka
## 25 = postgres
## 26 = keycloak
## 27 = minion (classic)
## 28 = metric processor
## 29 = lokahi-inventory
## 30 = events
## 31 = cortex
## 32 = prometheus
## 33 = datachoices
## 34 = minion-certificate-manager
## 35 = minion-certificate-verifier

# Tilt config #
config.define_string("listen-on")
config.define_string_list("args", args=True)
cfg = config.parse()
config.set_enabled_resources(cfg.get('args', []))

secret_settings(disable_scrub=True)  ## TODO: update secret values so we can reenable scrub
load('ext://uibutton', 'cmd_button', 'location')
load('ext://helm_remote', 'helm_remote') # for simple charts like jaeger

cmd_button(name='reload-certificates',
           argv=['sh', '-c', 'find target/tmp/ -type f -exec rm {} \\;'],
           text='Remove & reissue certificates',
           location=location.NAV,
           icon_name='sync')

# Functions #
cluster_arch_cmd = '$(tilt get cluster default -o=jsonpath --template="{.status.arch}")'

def jib_project(resource_name, image_name, base_path, k8s_resource_name, resource_deps=[], port_forwards=[], labels=None):
    """
    Builds and streams log output for our single-module Maven/Jib projects. Supports rapid development.

    NOTE: Do not use this function wtih multi-module Maven projects, use the jib_project_multi_module function instead.
    Rapid development does not work with multi-module Maven builds so it will appear to be broken.

    :param resource_name: Name of the Tilt resources to create.
    :param image_name: Name of the Docker image to build.
    :param base_path: Path to the project's main folder thats contains the root POM file.
    :param k8s_resource_name: Name of the Kubernetes Pod or Deployment to attach to the main Tilt resource.
    :param resource_deps: Adds extra dependencies to the main Tilt resource, which can be used for things like startup order.
    :param port_forwards: Specifies the port forwards to use on the main Tilt resource.
    :param labels: Adds labels to the Tilt resources to create categories in the sidebar.
    """
    if not labels:
        labels=[resource_name]


    compile_resource_name = '{}:live-reload'.format(resource_name)

    # This is the Tilt resource that compiles code for the purpose of rapid development.
    #
    # Triggers compilation when source files are changed. This produces compiled class files, which are copied in to the
    # running container by the main Tilt resource.
    #
    # Disable this resource to turn off live reload. Trigger the main Tilt resource for full builds instead.
    local_resource(
        compile_resource_name,
        'mvn clean compile -f {} -am'.format(base_path),
        deps=['{}/src'.format(base_path)],
        ignore=['**/target'],
        labels=labels,
    )


    # This is the image build part of the main Tilt resource.
    #
    # It will perform a full build and produces an image using Jib. This can be triggered manually when needed.
    #
    # Its live_update rules copy the project's resouces and compiled class files into the container. The web server
    # should be running spring-boot-devtools to trigger a restart when it detects these files changed. The "live-reload"
    # Tilt resource is responsible for triggering the compilation.
    #
    # Multi-module builds don't work with this function. The project's submodules are built into the container as jar
    # files and spring-boot-devtools's "restart" classloader needs to be configured with those jars. This varies per project.
    custom_build(
        image_name,
        'mvn clean install -DskipTests -Dapplication.docker.image=$EXPECTED_REF -f {} -Djib.from.platforms=linux/{} '.format(base_path, cluster_arch_cmd),
        deps=['{}/target/classes/org/opennms'.format(base_path), '{}/pom.xml'.format(base_path), '{}/src/main/resources'.format(base_path)],
        live_update=[
            sync('{}/target/classes/org/opennms'.format(base_path), '/app/classes/org/opennms'),
            sync('{}/src/main/resources'.format(base_path), '/app/resources'),
        ],
    )

    # This is the Kubernetes part of the main Tilt resource.
    #
    # Configures the name of the resource in Tilt, Kubernetes objects that should be part of it, and other settings.
    # The `k8s_resource_name` param should reference the Pod or Deployment.
    k8s_resource(
        k8s_resource_name,
        new_name=resource_name,
        labels=labels,
        resource_deps=resource_deps + [compile_resource_name],
        port_forwards=port_forwards,
    )

def jib_project_multi_module(resource_name, image_name, base_path, k8s_resource_name, resource_deps=[], port_forwards=[], labels=None, submodule=None):
    """
    Builds our multi-module Maven/Jib projects. Does not support rapid development.

    :param resource_name: Name of the Tilt resource to create.
    :param image_name: Name of the Docker image to build.
    :param base_path: Path to the project's main folder thats contains the root POM file.
    :param k8s_resource_name: Name of the Kubernetes Pod or Deployment to attach to the main Tilt resource.
    :param resource_deps: Adds extra dependencies to the Tilt resource, which can be used for things like startup order.
    :param port_forwards: Specifies the port forwards to use on the Tilt resource.
    :param labels: Adds labels to the Tilt resource to create categories in the sidebar.
    :param submodule: Specify a submodule of the Maven project, if needed.
    """
    if not labels:
        labels=[resource_name]

    submodule_flag = ''
    if (submodule):
        submodule_flag = '-pl {}'.format(submodule)

    # This is the image build part of the main Tilt resource.
    #
    # It will perform a full build and produces an image using Jib. This is triggered manually by default, but can be
    # triggered automatically on file change by changing the resource to "Auto" mode.
    custom_build(
        image_name,
        'mvn clean install -DskipTests -Dapplication.docker.image=$EXPECTED_REF -f {} -Djib.from.platforms=linux/{} {}'.format(base_path, cluster_arch_cmd, submodule_flag),
        deps=[base_path],
        ignore=['**/target'],
    )

    # This is the Kubernetes part of the main Tilt resource.
    #
    # Configures the name of the resource in Tilt, Kubernetes objects that should be part of it, and other settings.
    # The `k8s_resource_name` param should reference the Pod or Deployment.
    k8s_resource(
        k8s_resource_name,
        new_name=resource_name,
        labels=labels,
        resource_deps=resource_deps,
        port_forwards=port_forwards,
        trigger_mode=TRIGGER_MODE_MANUAL,
    )

def load_certificate_authority(secret_name, name, key_file_name, cert_file_name):
    local('./install-local/load-or-generate-secret.sh "default" {} {} {} {}'.format(name, secret_name, key_file_name, cert_file_name))
    watch_file(key_file_name)
    watch_file(cert_file_name)

def generate_certificate(secret_name, domain, ca_key_file_name, ca_cert_file_name):
    local('./install-local/generate-and-sign-certificate.sh "default" {} {} {} {}'.format(domain, secret_name, ca_key_file_name, ca_cert_file_name));

load_certificate_authority("root-ca-certificate", "opennms-ca", "target/tmp/server-ca.key", "target/tmp/server-ca.crt")
generate_certificate("opennms-minion-gateway-certificate", "minion.onmshs.local", "target/tmp/server-ca.key", "target/tmp/server-ca.crt")
generate_certificate("opennms-ui-certificate", "onmshs.local", "target/tmp/server-ca.key", "target/tmp/server-ca.crt")
load_certificate_authority("client-root-ca-certificate", "client-ca", "target/tmp/client-ca.key", "target/tmp/client-ca.crt")


# Deployment #
# https://github.com/jaegertracing/helm-charts/tree/main/charts/jaeger
helm_remote('jaeger', version='0.71.0', repo_url='https://jaegertracing.github.io/helm-charts', values="tilt-jaeger-values.yaml")
k8s_resource(
    'jaeger',
    labels=['0_useful'],
    port_forwards=port_forward(16686, name="Jaeger UI"),
)

k8s_yaml(
    helm(
        'charts/lokahi',
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
    ignore=['**/target','**/dependency-reduced-pom.xml'],
    labels=['shared'],
    resource_deps=['parent-pom'],
)

## Microservices ##
### Notification ###
jib_project(
    'notification',
    'opennms/lokahi-notification',
    'notifications',
    'opennms-notifications',
    port_forwards=['15065:6565', '15050:5005'],
)

### Vue.js App ###
#### UI ####
docker_build(
    'opennms/lokahi-ui',
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
    port_forwards=['17080:8080'],
    labels=['vuejs-app'],
)

#### BFF ####
jib_project(
    'vuejs-bff',
    'opennms/lokahi-rest-server',
    'rest-server',
    'opennms-rest-server',
    labels=['vuejs-app'],
    port_forwards=['13080:9090', '13050:5005'],
)

### Inventory ###
jib_project_multi_module(
    'inventory',
    'opennms/lokahi-inventory',
    'inventory',
    'opennms-inventory',
    port_forwards=['29080:8080', '29050:5005', '29065:6565'],
)

### Alert ###
jib_project(
    'alert',
    'opennms/lokahi-alert',
    'alert',
    'opennms-alert',
    port_forwards=['32080:9090', '32050:5005', '32065:6565',  '32000:8080'],
)

### Metrics Processor ###
jib_project_multi_module(
    'metrics-processor',
    'opennms/lokahi-metrics-processor',
    'metrics-processor',
    'opennms-metrics-processor',
    port_forwards=['28080:8080', '28050:5005'],
)

### Events ###
jib_project_multi_module(
    'events',
    'opennms/lokahi-events',
    'events',
    'opennms-events',
    port_forwards=['30050:5005', '30080:8080', '30065:6565'],
)

### Minion Gateway ###
jib_project_multi_module(
    'minion-gateway',
    'opennms/lokahi-minion-gateway',
    'minion-gateway',
    'opennms-minion-gateway',
    port_forwards=['16080:9090', '16050:5005'],
)

### DataChoices ###
jib_project(
    'datachoices',
    'opennms/lokahi-datachoices',
    'datachoices',
    'opennms-datachoices',
    port_forwards=['33080:9090', '33050:5005', '33065:6565'],
)

### Minion ###
custom_build(
    'opennms/lokahi-minion',
    'mvn install -f minion -Dapplication.docker.image=$EXPECTED_REF -DskipUTs=true -DskipITs=true -DskipTests=true -Dfeatures.verify.skip=true',
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

### Minion Certificate Manager ###
jib_project(
    'minion-certificate-manager',
    'opennms/lokahi-minion-certificate-manager',
    'minion-certificate-manager',
    'opennms-minion-certificate-manager',
    port_forwards=['34089:8990', '34050:5005'],
    resource_deps=['shared-lib']
)

# resource_name, image_name, base_path, k8s_resource_name, resource_deps=[], port_forwards=[], labels=None)
jib_project(
    'minion-certificate-verifier',
    'opennms/lokahi-minion-certificate-verifier',
    'minion-certificate-verifier',
    'opennms-minion-certificate-verifier',
    port_forwards=['35080:8080', '35050:5005'],
    resource_deps=['shared-lib']
)

## 3rd Party Resources ##
### Keycloak ###
docker_build(
    'opennms/lokahi-keycloak',
    'keycloak-ui',
    target='development',
    live_update=[
        sync('./keycloak-ui/themes', '/opt/keycloak/themes')
    ],
)
k8s_resource(
    'onms-keycloak',
    new_name='keycloak',
    labels='keycloak',
    port_forwards=['26080:8080'],
)

### Email ###
k8s_resource(
    'mail-server',
    labels='z_dependencies',
    port_forwards=['22080:8025'],
)

### Grafana ###
docker_build(
    'opennms/lokahi-grafana',
    'grafana',
)
k8s_resource(
    'grafana',
    labels='z_dependencies',
    port_forwards=['18080:3000'],
)

### Cortex ###
k8s_resource(
    'cortex',
    labels='z_dependencies',
    port_forwards=['19000:9000'],
)

### Postgres ###
k8s_resource(
    'postgres',
    labels='z_dependencies',
    port_forwards=['25054:5432'],
)

### Kafka ###
k8s_resource(
    'onms-kafka',
    new_name='kafka',
    labels='z_dependencies',
    port_forwards=['24092:24092'],
)

### Prometheus ###
k8s_resource(
    'prometheus',
    labels='z_dependencies',
    port_forwards=['32090:9090'],
)

### Others ###
listen_on = cfg.get('listen-on', "0.0.0.0")
k8s_resource(
    'ingress-nginx-controller',
    labels=['0_useful'],
    port_forwards=[
        port_forward(8123, 80, host=listen_on),
        port_forward(1443, 443, host=listen_on),
    ],
    links=[
        link("https://onmshs.local:1443/", name="Web UI"),
    ],
)

k8s_resource(
    'opennms-nginx-errors',
    labels=['opennms-nginx-errors'],
)

k8s_resource(
    'ingress-nginx-admission-create',
    labels=['z_ingress-nginx-support'],
)

k8s_resource(
    'ingress-nginx-admission-patch',
    labels=['z_ingress-nginx-support'],
)
