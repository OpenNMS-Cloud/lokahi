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
## 25 = citus/postgres
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
config.define_string('listen-on')
config.define_string_list('values')
config.define_string_list('args', args=True)
config.define_string_list('devmode')
cfg = config.parse()
config.set_enabled_resources(cfg.get('args', []))
devmode_list = cfg.get('devmode', [])

secret_settings(disable_scrub=True)  ## TODO: update secret values so we can reenable scrub
load('ext://uibutton', 'cmd_button', 'location')
load('ext://helm_remote', 'helm_remote') # for simple charts like tempo and cert-manager
load('ext://helm_resource', 'helm_resource', 'helm_repo') # for charts that we want to have resources for

cmd_button(name='reload-certificates',
           argv=['sh', '-c', 'find target/tmp/ -type f -exec rm {} \\; ; kubectl --context ' + k8s_context() + ' -n ' + k8s_namespace() + ' delete secret root-ca-certificate opennms-minion-gateway-certificate opennms-ui-certificate client-root-ca-certificate ; kubectl --context ' + k8s_context() + ' -n ' + k8s_namespace() + ' rollout restart deployment opennms-minion'],
           text='Remove & reissue certificates',
           location=location.NAV,
           icon_name='sync')

cmd_button(name='reload-helm',
           argv=['touch', 'tilt-helm-values.yaml'],
           text='Helm upgrade',
           location=location.NAV,
           icon_name='system_update_alt')

# Give ourselves more time -- this needs to include enough time to download container images (cert-manager, nginx, etc.)
# if we don't have them locally. We try to be nice to people with slower connections.
update_settings(k8s_upsert_timeout_secs=600)
if os.getenv('CI'):
    # Be a little bit more aggressive in CI
    update_settings(max_parallel_updates=4)

# Functions #
cluster_arch_cmd = '$(tilt get cluster default -o=jsonpath --template="{.status.arch}")'

def jib_project(resource_name, image_name, base_path, k8s_resource_name, resource_deps=[], port_forwards=[], links=[], labels=None):
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
        resource_deps=resource_deps,
        links=links,
        port_forwards=port_forwards,
        trigger_mode=TRIGGER_MODE_MANUAL,
    )

def jib_project_multi_module(resource_name, image_name, base_path, k8s_resource_name, resource_deps=[], port_forwards=[], links=[], labels=None, submodule=None):
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
        links=links,
        trigger_mode=TRIGGER_MODE_MANUAL,
    )

def load_certificate_authority(secret_name, name, key_file_name, cert_file_name):
    local([
        './install-local/load-or-generate-secret.sh',
        name,
        secret_name,
        key_file_name,
        cert_file_name,
        '--context',
        k8s_context(),
        '-n',
        k8s_namespace(),
    ])
    watch_file(key_file_name)
    watch_file(cert_file_name)

def generate_certificate(secret_name, domain, ca_key_file_name, ca_cert_file_name):
    local([
        './install-local/generate-and-sign-certificate.sh',
        domain,
        secret_name,
        ca_key_file_name,
        ca_cert_file_name,
        '--context',
        k8s_context(),
        '-n',
        k8s_namespace(),
    ])

def ssl_check(domain, port, tries=1, check_http=True, deps=None, resource_deps=None):
    cmd = [
        './tools/ssl-check.sh',
        '-t',
        str(tries),
    ]

    if not check_http:
        cmd = cmd + ['-s']

    cmd = cmd + [
        domain,
        str(port),
        '--context',
        k8s_context(),
        '-n',
        k8s_namespace(),
    ]

    if resource_deps:
        local_resource('ssl_check', cmd, deps=deps, resource_deps=resource_deps, labels='z_dependencies')
    else:
        local(cmd)

# If you don't specify a resource, the button will be added to the global nav (location.NAV).
def create_devmode_toggle_btn(devmode_key, resource=None):
    # we should not mutate new_config so we need to work with a copy
    new_config = {}
    new_config.update(cfg)
    new_config.update({'devmode': get_toggled_devmode_list(devmode_key, devmode_list)})

    if resource:
        kvargs = {
            'resource': resource,
            'text': 'Toggle Dev Mode',
            'name': 'toggle-resource-{}-devmode'.format(resource),
        }
    else:
        kvargs = {
            'location': location.NAV,
            'text': 'Toggle Dev Mode - ' + devmode_key,
            'name': 'toggle-global-{}-devmode'.format(devmode_key),
        }

    cmd_button(
        argv=['sh', '-c', 'printenv CONFIG > tilt_config.json'],
        env=[
            'CONFIG={}'.format(encode_json(new_config))
        ],
        icon_name='code_off' if is_devmode_enabled(devmode_key) else 'code_block',
        **kvargs
    )

def get_toggled_devmode_list(resource_name, original_list):
    # we should not mutate original_list so we need to work with a copy
    result = []
    result.extend(original_list)

    if (resource_name in original_list):
        result.remove(resource_name)
    else:
        result.append(resource_name)

    return result

def is_devmode_enabled(devmode_key):
    return devmode_key in devmode_list;


# Setup certificates #
load_certificate_authority('root-ca-certificate', 'opennms-ca', 'target/tmp/server-ca.key', 'target/tmp/server-ca.crt')
generate_certificate('opennms-minion-gateway-certificate', 'minion.onmshs.local', 'target/tmp/server-ca.key', 'target/tmp/server-ca.crt')
generate_certificate('opennms-ui-certificate', 'onmshs.local', 'target/tmp/server-ca.key', 'target/tmp/server-ca.crt')
load_certificate_authority('client-root-ca-certificate', 'client-ca', 'target/tmp/client-ca.key', 'target/tmp/client-ca.crt')

# Do a quick sanity check on certificates, but skip HTTP checks for now since the ingress, etc. aren't up yet
ssl_check('onmshs.local', 1443, check_http=False)

# We wait to do the full HTTP checks once ingress-nginx, UI, and minion-gateway are up
ssl_check('onmshs.local', 1443, tries=300,
    deps=[
        'target/tmp/server-ca.key',
        'target/tmp/server-ca.crt',
        'target/tmp/client-ca.key',
        'target/tmp/client-ca.crt',
    ],
    resource_deps=[
        'ingress-nginx',
        'ui:prod',
        'minion-gateway',
    ]
)


# Deployment #
# https://github.com/grafana/helm-charts/tree/main/charts/tempo
helm_remote('tempo', version='1.7.1', repo_url='https://grafana.github.io/helm-charts',
    set=[
        'tempo.metricsGenerator.enabled=True',
        'tempo.metricsGenerator.remoteWriteUrl=http://prometheus:9090/prometheus/api/v1/write',
    ]
)
k8s_resource(
    'tempo',
    labels=['0_useful'],
    port_forwards=port_forward(3100, name='Tempo HTTP API'), # https://grafana.com/docs/tempo/latest/api_docs/
    links=[
        link('https://onmshs.local:1443/grafana/explore?orgId=1&left=%7B%22datasource%22:%22tempo%22,%22queries%22:%5B%7B%22refId%22:%22A%22,%22datasource%22:%7B%22type%22:%22tempo%22,%22uid%22:%22tempo%22%7D,%22queryType%22:%22traceql%22,%22limit%22:20,%22query%22:%22%7B%7D%22%7D%5D,%22range%22:%7B%22from%22:%22now-1h%22,%22to%22:%22now%22%7D%7D', 'Grafana - Explore Tempo'),
    ]
)
k8s_resource(
    'tempo',
    objects=['tempo:serviceaccount', 'tempo:configmap', ],
)

# Deployment #
helm_repo('jetstack', 'https://charts.jetstack.io', labels=['z_dependencies'])
helm_resource('cert-manager', 'jetstack/cert-manager',
	flags=[
		'--version=1.11.0',
		'--set', 'installCRDs=true',
		'--set', 'cainjector.extraArgs={--leader-elect=false}',
	],
	resource_deps=[
		'jetstack',
	],
)
k8s_resource(
    'cert-manager',
    labels=['z_dependencies'],
)

# https://github.com/kubernetes/ingress-nginx/tree/main/charts/ingress-nginx
helm_repo('ingress-nginx-repo', 'https://kubernetes.github.io/ingress-nginx' , labels=['z_dependencies'])
helm_resource('ingress-nginx', 'ingress-nginx-repo/ingress-nginx',
	flags=[
		'--version=4.7.0',
		'--values=tilt-ingress-nginx-values.yaml',
		'--timeout=60s'
	],
	deps=['Tiltfile', 'tilt-ingress-nginx-values.yaml'],
	resource_deps=[
		'cert-manager',
		'ingress-nginx-repo',
	],
)

# Deployment #
metricsServerDevmodeKey = 'metrics-server'
create_devmode_toggle_btn(metricsServerDevmodeKey)
if is_devmode_enabled(metricsServerDevmodeKey):
    # https://gist.github.com/sanketsudake/a089e691286bf2189bfedf295222bd43?permalink_comment_id=4458547#gistcomment-4458547
    helm_repo('metrics-server-repo', 'https://kubernetes-sigs.github.io/metrics-server/', labels=['z_dependencies'])
    helm_resource('metrics-server', 'metrics-server-repo/metrics-server',
        namespace='kube-system',
        flags=[
#            '--version=1.11.0',
            '--set', 'args={--kubelet-insecure-tls}',
        ],
        resource_deps=[
            'metrics-server-repo',
        ],
    )
    k8s_resource(
        'metrics-server',
        labels=['z_dependencies'],
    )
    create_devmode_toggle_btn(metricsServerDevmodeKey, resource='metrics-server')

k8s_yaml(
    helm(
        'charts/lokahi',
        values=['./tilt-helm-values.yaml'] + cfg.get('values', []),
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
    trigger_mode=TRIGGER_MODE_MANUAL,
)

k8s_resource(
    new_name='shared-kube',
    objects=['spring-boot-app-config:configmap', 'spring-boot-env:configmap', 'opennms-ingress:ingress'],
    labels='shared',
)

## Microservices ##
### Notification ###
jib_project(
    'notification',
    'opennms/lokahi-notification',
    'notifications',
    'opennms-notifications',
    port_forwards=['15065:6565', '15050:5005', '15080:8080'],
    resource_deps=['shared-lib', 'citus-worker'],
)

### Vue.js App ###
#### UI - Local development server ####
uiDevmodeKey = 'ui'
if is_devmode_enabled(uiDevmodeKey):
    serve_env={
        'VITE_BASE_URL': 'https://onmshs.local:1443/api',
        'VITE_KEYCLOAK_URL': 'https://onmshs.local:1443/auth'
    }
    local_resource(
        'ui:dev',
        cmd='yarn install',
        dir='ui',
        serve_cmd='yarn run dev',
        serve_dir='ui',
        serve_env=serve_env,
        labels=['ui'],
        links=[
            link('http://onmshs.local:8080/', 'Web UI (yarn run dev)')
        ]
    )
    create_devmode_toggle_btn(uiDevmodeKey, resource='ui:dev')

#### UI - Production container ####
docker_build(
    'opennms/lokahi-ui',
    'ui',
)
#target='production', # To simulate production for debugging pipeline issues.

k8s_resource(
    'opennms-ui',
    new_name='ui:prod',
    labels=['ui'],
    trigger_mode=TRIGGER_MODE_MANUAL if is_devmode_enabled(uiDevmodeKey) else TRIGGER_MODE_AUTO,
    links=[
        link('https://onmshs.local:1443/', 'Web UI (prod container)')
    ],
)

create_devmode_toggle_btn(uiDevmodeKey, resource='ui:prod')

#### BFF ####
jib_project(
    'rest-server',
    'opennms/lokahi-rest-server',
    'rest-server',
    'opennms-rest-server',
    labels=['rest-server'],
    port_forwards=['13080:9090', '13050:5005'],
    links=[
      link('https://onmshs.local:1443/api/graphql', 'GraphQL Endpoint'),
      link('https://onmshs.local:1443/api/gui', 'GraphQL Playground'),
    ],
    resource_deps=['shared-lib'],
)

### Inventory ###
jib_project_multi_module(
    'inventory',
    'opennms/lokahi-inventory',
    'inventory',
    'opennms-inventory',
    port_forwards=['29080:8080', '29050:5005', '29065:6565'],
    resource_deps=['shared-lib', 'citus-worker'],
)
k8s_resource(
    'inventory',
    objects=['opennms-inventory-encryption-key:secret'],
)

### Alert ###
jib_project(
    'alert',
    'opennms/lokahi-alert',
    'alert',
    'opennms-alert',
    port_forwards=['32080:9090', '32050:5005', '32065:6565',  '32000:8080'],
    resource_deps=['shared-lib', 'citus-worker'],
)

### Metrics Processor ###
jib_project_multi_module(
    'metrics-processor',
    'opennms/lokahi-metrics-processor',
    'metrics-processor',
    'opennms-metrics-processor',
    port_forwards=['28080:8080', '28050:5005'],
    resource_deps=['shared-lib', 'citus-worker'],
)

### Events ###
jib_project_multi_module(
    'events',
    'opennms/lokahi-events',
    'events',
    'opennms-events',
    port_forwards=['30050:5005', '30080:8080', '30065:6565'],
    resource_deps=['shared-lib', 'citus-worker'],
)

### Minion Gateway ###
jib_project_multi_module(
    'minion-gateway',
    'opennms/lokahi-minion-gateway',
    'minion-gateway',
    'opennms-minion-gateway',
    port_forwards=['16080:8080', '16050:5005'],
    resource_deps=['shared-lib', 'citus-worker'],
)
k8s_resource(
    'minion-gateway',
    objects=['opennms-minion-gateway-sa:serviceaccount', 'opennms-minion-gateway-rb:rolebinding', 'minion-gateway-ignite-config:configmap', 'opennms-minion-gateway:ingress' ],
)

### DataChoices ###
jib_project(
    'datachoices',
    'opennms/lokahi-datachoices',
    'datachoices',
    'opennms-datachoices',
    port_forwards=['33080:9090', '33050:5005', '33065:6565'],
    resource_deps=['shared-lib', 'citus-worker'],
)

### Minion ###
custom_build(
    'opennms/lokahi-minion',
    'mvn clean install -f minion -Dapplication.docker.image=$EXPECTED_REF -DskipUTs=true -DskipITs=true -DskipTests=true -Dfeatures.verify.skip=true',
    deps=['./minion'],
    ignore=['**/target', '**/dependency-reduced-pom.xml'],
)

k8s_resource(
    'opennms-minion',
    new_name='minion',
    port_forwards=['12022:8101', '12080:8181', '12050:5005'],
    labels=['minion'],
    trigger_mode=TRIGGER_MODE_MANUAL,
    resource_deps=['shared-lib'],
)
k8s_resource(
    'minion',
    objects=['opennms-minion-sa:serviceaccount', 'opennms-minion-rb:rolebinding', 'minion-scripts:configmap', 'role-endpoints:role'],
)
cmd_button(
    name='button-opennms-minion',
    text='Rollout restart minion',
    resource='minion',
    argv=[
        'kubectl',
        '--context', k8s_context(),
        '-n', k8s_namespace(),
        'rollout', 'restart', 'deployment', 'opennms-minion',
    ],
    icon_name='sync',
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
k8s_resource(
    'minion-certificate-manager',
    objects=['minion-certificate-manager-pvc:persistentvolumeclaim'],
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
    resource_deps=['citus-worker'],
    links=[
      link('https://onmshs.local:1443/auth/admin/', 'Admin Console'),
      link('http://localhost:26080/auth', 'Welcome Page')
    ]
)
k8s_resource(
    'keycloak',
    objects=['keycloak-realm-configmap:configmap', 'onms-keycloak-initial-admin:secret', ],
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
    resource_deps=['citus-worker'],
)
k8s_resource(
    'grafana',
    objects=['grafana:secret'],
)

### Cortex ###
k8s_resource(
    'cortex',
    labels='z_dependencies',
    port_forwards=['19000:9000'],
    links=[
        link('https://onmshs.local:1443/grafana/explore?orgId=1&left=%7B%22datasource%22:%22EdAkOOOSk%22,%22queries%22:%5B%7B%22refId%22:%22A%22,%22expr%22:%22%22,%22range%22:true,%22instant%22:true,%22datasource%22:%7B%22type%22:%22prometheus%22,%22uid%22:%22EdAkOOOSk%22%7D%7D%5D,%22range%22:%7B%22from%22:%22now-1h%22,%22to%22:%22now%22%7D%7D', 'Grafana - Explore Metrics'),
    ]
)
k8s_resource(
    'cortex',
    objects=['cortex-config-map:configmap'],
)

### Citus/Postgres ###
k8s_resource(
    'citus',
    labels='z_dependencies',
    port_forwards=['25054:5432'],
    resource_deps=['cert-manager'],
    links=[
        link('jdbc:postgresql://localhost:25054/desenv?user=desenv&password=any', name='JDBC URL'),
    ]
)

k8s_resource(
    'citus-worker',
    labels='z_dependencies',
    resource_deps=['citus'],
)

k8s_resource(
    'citus',
    objects=['citus-issuer:issuer', 'citus-cert:certificate', 'citus-conf:configmap', 'postgres:secret', 'citus-initial-sql:secret'],
)

k8s_resource(
    'citus-worker',
    objects=['citus-worker-conf:configmap'],
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
    links=[
        link('https://onmshs.local:1443/grafana/explore?orgId=1&left=%7B%22datasource%22:%22zK0kOddIk%22,%22queries%22:%5B%7B%22refId%22:%22A%22,%22expr%22:%22%22,%22range%22:true,%22instant%22:true,%22datasource%22:%7B%22type%22:%22prometheus%22,%22uid%22:%22zK0kOddIk%22%7D%7D%5D,%22range%22:%7B%22from%22:%22now-1h%22,%22to%22:%22now%22%7D%7D', 'Grafana - Explore Internal Metrics'),
    ]
)
k8s_resource(
    'prometheus',
    objects=['prometheus-sa:serviceaccount', 'prometheus:clusterrole', 'prometheus:clusterrolebinding', 'prometheus-config-map:configmap'],
)

### Others ###
listen_on = cfg.get('listen-on', '0.0.0.0')
k8s_resource(
    'ingress-nginx',
    labels=['0_useful'],
    port_forwards=[
        port_forward(8123, 80, host=listen_on),
        port_forward(1443, 443, host=listen_on),
    ],
    links=[
        link('https://onmshs.local:1443/', name='Web UI (prod container)'),
    ],
)
