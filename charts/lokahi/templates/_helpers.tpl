{{- /*
lokahi.image: return the docker image name including tag for the service
argument: a dictionary with Values and "thisService" rooted at the values
for the service.
example:
	image: {{ include "lokahi.image" (dict "Values" .Values "thisService" .Values.OpenNMS.Alert) | quote }}

Precedence for image (first wins):
1. thisService.Image
2. OpenNMS.global.image.repository + "/" + image short name + "/" + tag

image short name:
1. thisService.ImageShortName
2. thisService.ServiceName

tag:
1. OpenNMS.global.image.tag
2. "latest"

TODO: The default tag should eventually be .Chart.AppVersion, but we
aren't versioning the chart yet.
*/}}
{{- define "lokahi.image" }}
{{- if .thisService.Image -}}
{{- .thisService.Image -}}
{{- else -}}
{{- $imageShortName := .thisService.ImageShortName | default .thisService.ServiceName -}}
{{- $tag := .Values.OpenNMS.global.image.tag | default "latest" -}}
{{- printf "%s/%s:%s" .Values.OpenNMS.global.image.repository $imageShortName $tag -}}
{{- end -}}
{{- end -}}

{{- /*
lokahi.deployment.env: return a subset of a pod spec "env" section with common environment
variables and any service-specific overrides.
argument: (like lokahi.image above) a dictionary with Values and "thisService" rooted at the
values for the service.
example:
	env:
	  ... normal env settings should come first ...
	  # Do not put any env variables below this. The lokahi.development.env include should be last
	  # in the 'env' section so variables can be overridden with Helm chart values when needed.
	  {{- include "lokahi.deployment.env" (dict "Values" .Values "thisService" .Values.OpenNMS.Events) | nindent 12 }}

Note: when this is included, the the lokahi.development.env include should be last thing
in the 'env' section so variables can be overridden with Helm chart values when needed
by adding them as key/value pairs under <ServiceName>.env.
*/}}
{{- define "lokahi.deployment.env" -}}
  {{- /* OpenTelemetry environment variables */ -}}
- name: OTEL_SERVICE_NAME
  value: {{ .thisService.ServiceName | quote }}
- name: OTEL_RESOURCE_ATTRIBUTES
  value: {{ printf "service.version=%s" (regexReplaceAllLiteral ".*:" (include "lokahi.image" .) "") | quote }}
  {{- /* Other environment variables */ -}}
  {{- if .thisService.env }}
    {{- range $key, $val := .thisService.env }}
- name: {{ $key }}
  value: {{ $val | quote }}
    {{- end }}
  {{- end }}
{{- end }}

{{- define "lokahi.kafkaSecretFrom" -}}
  {{- if .kafkaSecretName }}
- secretRef:
    name: {{ .kafkaSecretName }}
  {{- end }}
{{- end }}
