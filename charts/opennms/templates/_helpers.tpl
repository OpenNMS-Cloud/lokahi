{{- define "deployment.env" -}}
- name: OTEL_SERVICE_NAME
  value: {{ .ServiceName | quote }}
  {{- if (regexMatch ".*:" .Image) }}
- name: OTEL_RESOURCE_ATTRIBUTES
  value: {{ printf "service.version=%s" (regexReplaceAllLiteral ".*:" .Image "") | quote }}
  {{- end }}
  {{- if .env }}
    {{- range $key, $val := .env }}
- name: {{ $key }}
  value: {{ $val | quote }}
    {{- end }}
  {{- end }}
{{- end }}
