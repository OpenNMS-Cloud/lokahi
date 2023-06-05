{{- define "deployment.env" -}}
  {{- /* OpenTelemetry environment variables */ -}}
- name: OTEL_SERVICE_NAME
  value: {{ .ServiceName | quote }}
  {{- if (regexMatch ".*:" .Image) }}
- name: OTEL_RESOURCE_ATTRIBUTES
  value: {{ printf "service.version=%s" (regexReplaceAllLiteral ".*:" .Image "") | quote }}
  {{- end }}
  {{- /* Other environment variables */ -}}
  {{- if .env }}
    {{- range $key, $val := .env }}
- name: {{ $key }}
  value: {{ $val | quote }}
    {{- end }}
  {{- end }}
{{- end }}

{{- define "kafkaSecretFrom" -}}
  {{- if .kafkaSecretName }}
- secretRef:
    name: {{ .kafkaSecretName }}
  {{- end }}
{{- end }}
