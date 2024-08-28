{{/* vim: set filetype=mustache: */}}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-auto-config-consistency.name" }}
  {{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-oss-auto-config-consistency.version" }}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Define the service id.
*/}}
{{- define "eric-oss-auto-config-consistency.serviceId" -}}
rapp-{{ include "eric-oss-auto-config-consistency.name" . -}}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-auto-config-consistency.fullname" -}}
{{- if .Values.fullnameOverride -}}
  {{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
  {{- $name := default .Chart.Name .Values.nameOverride -}}
  {{- printf "%s" $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Create the name of the postgres secret to use
*/}}
{{- define "eric-oss-auto-config-consistency.pgSecretName" -}}
{{- (include "eric-oss-auto-config-consistency.fullname" .) }}-postgres-secret
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-oss-auto-config-consistency.chart" }}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create image pull secrets for global (outside of scope)
*/}}
{{- define "eric-oss-auto-config-consistency.pullSecret.global" -}}
{{- $pullSecret := "" -}}
{{- if .Values.global -}}
  {{- if .Values.global.pullSecret -}}
    {{- $pullSecret = .Values.global.pullSecret -}}
  {{- end -}}
  {{- end -}}
{{- print $pullSecret -}}
{{- end -}}

{{/*
Create image pull secret, service level parameter takes precedence
*/}}
{{- define "eric-oss-auto-config-consistency.pullSecret" -}}
{{- $pullSecret := (include "eric-oss-auto-config-consistency.pullSecret.global" . ) -}}
{{- if .Values.imageCredentials -}}
  {{- if .Values.imageCredentials.pullSecret -}}
    {{- $pullSecret = .Values.imageCredentials.pullSecret -}}
  {{- end -}}
{{- end -}}
{{- print $pullSecret -}}
{{- end -}}

{{/*
Timezone variable
*/}}
{{- define "eric-oss-auto-config-consistency.timezone" }}
  {{- $timezone := "UTC" }}
  {{- if .Values.global }}
    {{- if .Values.global.timezone }}
      {{- $timezone = .Values.global.timezone }}
    {{- end }}
  {{- end }}
  {{- print $timezone | quote }}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "eric-oss-auto-config-consistency.common-labels" }}
app.kubernetes.io/name: {{ include "eric-oss-auto-config-consistency.name" . }}
helm.sh/chart: {{ include "eric-oss-auto-config-consistency.chart" . }}
{{ include "eric-oss-auto-config-consistency.selectorLabels" . }}
app.kubernetes.io/version: {{ include "eric-oss-auto-config-consistency.version" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Create a user defined label (DR-D1121-068, DR-D1121-060)
*/}}
{{ define "eric-oss-auto-config-consistency.config-labels" }}
  {{- $global := (.Values.global).labels -}}
  {{- $service := .Values.labels -}}
  {{- include "eric-oss-auto-config-consistency.mergeLabels" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end }}

{{/*
Return the fsgroup set via global parameter if it's set, otherwise 10000
*/}}
{{- define "eric-oss-auto-config-consistency.fsGroup.coordinated" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.fsGroup -}}
      {{- if .Values.global.fsGroup.manual -}}
        {{ .Values.global.fsGroup.manual }}
      {{- else -}}
        {{- if eq .Values.global.fsGroup.namespace true -}}
          # The 'default' defined in the Security Policy will be used.
        {{- else -}}
          10000
      {{- end -}}
    {{- end -}}
  {{- else -}}
    10000
  {{- end -}}
  {{- else -}}
    10000
  {{- end -}}
{{- end -}}

{{/*
Selector labels
*/}}
{{- define "eric-oss-auto-config-consistency.selectorLabels" -}}
app.kubernetes.io/name: {{ include "eric-oss-auto-config-consistency.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "eric-oss-auto-config-consistency.serviceAccountName" -}}
  {{- if .Values.serviceAccount.create }}
    {{- default (include "eric-oss-auto-config-consistency.fullname" .) .Values.serviceAccount.name }}
  {{- else }}
    {{- default "default" .Values.serviceAccount.name }}
  {{- end }}
{{- end }}

{{/*
Create AppArmor annotations
*/}}
{{- define "eric-oss-auto-config-consistency.AppArmor-annotations" }}
{{- $appArmorValue := .Values.appArmorProfile.type -}}
    {{- if .Values.appArmorProfile -}}
        {{- if .Values.appArmorProfile.type -}}
            {{- if eq .Values.appArmorProfile.type "localhost" -}}
                {{- $appArmorValue = printf "%s/%s" .Values.appArmorProfile.type .Values.appArmorProfile.localhostProfile }}
            {{- end}}
container.apparmor.security.beta.kubernetes.io/eric-oss-auto-config-consistency: {{ $appArmorValue | quote }}
container.apparmor.security.beta.kubernetes.io/eric-oss-auto-config-consistency-ui: {{ $appArmorValue | quote }}
        {{- end}}
    {{- end}}
{{- end}}

{{/*
Seccomp profile section (DR-1123-128)
*/}}
{{- define "eric-oss-auto-config-consistency.seccomp-profile" }}
    {{- if .Values.seccompProfile }}
      {{- if .Values.seccompProfile.type }}
          {{- if eq .Values.seccompProfile.type "Localhost" }}
              {{- if .Values.seccompProfile.localhostProfile }}
seccompProfile:
  type: {{ .Values.seccompProfile.type }}
  localhostProfile: {{ .Values.seccompProfile.localhostProfile }}
            {{- end }}
          {{- else }}
seccompProfile:
  type: {{ .Values.seccompProfile.type }}
          {{- end }}
        {{- end }}
      {{- end }}
{{- end }}

{{/*
Annotations for Product Name and Product Number (DR-D1121-064).
*/}}
{{- define "eric-oss-auto-config-consistency.product-info" }}
ericsson.com/product-name: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productName | quote }}
ericsson.com/product-number: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productNumber | quote }}
ericsson.com/product-revision: {{regexReplaceAll "(.*)[+|-].*" .Chart.Version "${1}" | quote }}
{{- end }}

{{/*
Create a user defined annotation (DR-D1121-065, DR-D1121-060)
*/}}
{{ define "eric-oss-auto-config-consistency.config-annotations" }}
  {{- $global := (.Values.global).annotations -}}
  {{- $service := .Values.annotations -}}
  {{- include "eric-oss-auto-config-consistency.mergeAnnotations" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end }}

{{/*
Create prometheus info
*/}}
{{- define "eric-oss-auto-config-consistency.prometheus" -}}
prometheus.io/path: {{ .Values.prometheus.path | quote }}
prometheus.io/port: {{ .Values.service.port | quote }}
prometheus.io/scrape: {{ .Values.prometheus.scrape | quote }}
{{- end -}}

{{/*
Create log control configmap name.
*/}}
{{- define "eric-oss-auto-config-consistency.log-control-configmap.name" }}
  {{- include "eric-oss-auto-config-consistency.name.noQuote" . | printf "%s-log-control-configmap" | quote }}
{{- end }}

{{/*
Define the role reference for security policy
*/}}
{{- define "eric-oss-auto-config-consistency.securityPolicy.reference" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.security -}}
      {{- if .Values.global.security.policyReferenceMap -}}
        {{ $mapped := index .Values "global" "security" "policyReferenceMap" "default-restricted-security-policy" }}
        {{- if $mapped -}}
          {{ $mapped }}
        {{- else -}}
          default-restricted-security-policy
        {{- end -}}
      {{- else -}}
        default-restricted-security-policy
      {{- end -}}
    {{- else -}}
      default-restricted-security-policy
    {{- end -}}
  {{- else -}}
    default-restricted-security-policy
  {{- end -}}
{{- end -}}

{{/*
Define the annotations for security policy
*/}}
{{- define "eric-oss-auto-config-consistency.securityPolicy.annotations" -}}
# Automatically generated annotations for documentation purposes.
{{- end -}}

{{/*
Define Pod Disruption Budget value taking into account its type (int or string)
*/}}
{{- define "eric-oss-auto-config-consistency.pod-disruption-budget" -}}
  {{- if kindIs "string" .Values.podDisruptionBudget.minAvailable -}}
    {{- print .Values.podDisruptionBudget.minAvailable | quote -}}
  {{- else -}}
    {{- print .Values.podDisruptionBudget.minAvailable | atoi -}}
  {{- end -}}
{{- end -}}

{{/*
Define upper limit for TerminationGracePeriodSeconds
*/}}
{{- define "eric-oss-auto-config-consistency.terminationGracePeriodSeconds" -}}
{{- if .Values.terminationGracePeriodSeconds -}}
  {{- toYaml .Values.terminationGracePeriodSeconds -}}
{{- end -}}
{{- end -}}

{{/*
Define tolerations to comply to DR-D1120-060
*/}}
{{- define "eric-oss-auto-config-consistency.tolerations" -}}
{{- if .Values.tolerations -}}
  {{- toYaml .Values.tolerations -}}
{{- end -}}
{{- end -}}

{{/*
Create a merged set of nodeSelectors from global and service level.
*/}}
{{- define "eric-oss-auto-config-consistency.nodeSelector" -}}
{{- $globalValue := (dict) -}}
{{- if .Values.global -}}
    {{- if .Values.global.nodeSelector -}}
      {{- $globalValue = .Values.global.nodeSelector -}}
    {{- end -}}
{{- end -}}
{{- if .Values.nodeSelector -}}
  {{- range $key, $localValue := .Values.nodeSelector -}}
    {{- if hasKey $globalValue $key -}}
         {{- $Value := index $globalValue $key -}}
         {{- if ne $Value $localValue -}}
           {{- printf "nodeSelector \"%s\" is specified in both global (%s: %s) and service level (%s: %s) with differing values which is not allowed." $key $key $globalValue $key $localValue | fail -}}
         {{- end -}}
     {{- end -}}
    {{- end -}}
    nodeSelector: {{- toYaml (merge $globalValue .Values.nodeSelector) | trim | nindent 2 -}}
{{- else -}}
  {{- if not ( empty $globalValue ) -}}
    nodeSelector: {{- toYaml $globalValue | trim | nindent 2 -}}
  {{- end -}}
{{- end -}}
{{- end -}}

{{/*
    Define Image Pull Policy
*/}}
{{- define "eric-oss-auto-config-consistency.registryImagePullPolicy" -}}
    {{- $globalRegistryPullPolicy := "IfNotPresent" -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.imagePullPolicy -}}
                {{- $globalRegistryPullPolicy = .Values.global.registry.imagePullPolicy -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- print $globalRegistryPullPolicy -}}
{{- end -}}

{{/*
    Define Image Pull Policy for UI
*/}}
{{- define "eric-oss-auto-config-consistency-ui.registryImagePullPolicy" -}}
    {{- $globalRegistryPullPolicy := "IfNotPresent" -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.imagePullPolicy -}}
                {{- $globalRegistryPullPolicy = .Values.global.registry.imagePullPolicy -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- print $globalRegistryPullPolicy -}}
{{- end -}}

{/*
Define JVM heap size (DR-D1126-010 | DR-D1126-011)
*/}}
{{- define "eric-oss-auto-config-consistency.jvmHeapSettings" -}}
    {{- $initRAM := "" -}}
    {{- $maxRAM := "" -}}
    {{- $heapDump := "-XX:+HeapDumpOnOutOfMemoryError" -}}
    {{- $heapDumpPath := "-XX:HeapDumpPath=/heap-dumps/eric-oss-auto-config-consistency.hprof" -}}
    {{- $exitOnError := "-XX:+ExitOnOutOfMemoryError" -}}

    {{/*
       ramLimit is set by default to 1.0, this is if the service is set to use anything less than M/Mi
       Rather than trying to cover each type of notation,
       if a user is using anything less than M/Mi then the assumption is its less than the cutoff of 1.3GB
       */}}
    {{- $ramLimit := 1.0 -}}
    {{- $ramComparison := 1.3 -}}

    {{- if not (index .Values "resources" "eric-oss-auto-config-consistency" "limits" "memory") -}}
        {{- fail "memory limit for eric-oss-auto-config-consistency is not specified" -}}
    {{- end -}}

    {{- if (hasSuffix "Gi" (index .Values "resources" "eric-oss-auto-config-consistency" "limits" "memory")) -}}
        {{- $ramLimit = trimSuffix "Gi" (index .Values "resources" "eric-oss-auto-config-consistency" "limits" "memory") | float64 -}}
    {{- else if (hasSuffix "G" (index .Values "resources" "eric-oss-auto-config-consistency" "limits" "memory")) -}}
        {{- $ramLimit = trimSuffix "G" (index .Values "resources" "eric-oss-auto-config-consistency" "limits" "memory") | float64 -}}
    {{- else if (hasSuffix "Mi" (index .Values "resources" "eric-oss-auto-config-consistency" "limits" "memory")) -}}
        {{- $ramLimit = (div (trimSuffix "Mi" (index .Values "resources" "eric-oss-auto-config-consistency" "limits" "memory") | float64) 1000) | float64  -}}
    {{- else if (hasSuffix "M" (index .Values "resources" "eric-oss-auto-config-consistency" "limits" "memory")) -}}
        {{- $ramLimit = (div (trimSuffix "M" (index .Values "resources" "eric-oss-auto-config-consistency" "limits" "memory")| float64) 1000) | float64  -}}
    {{- end -}}


    {{- if (index .Values "resources" "eric-oss-auto-config-consistency" "jvm") -}}
        {{- if (index .Values "resources" "eric-oss-auto-config-consistency" "jvm" "initialMemoryAllocationPercentage") -}}
            {{- $initRAM = (index .Values "resources" "eric-oss-auto-config-consistency" "jvm" "initialMemoryAllocationPercentage") | int -}}
            {{- $initRAM = printf "-XX:InitialRAMPercentage=%d" $initRAM -}}
        {{- else -}}
            {{- fail "initialMemoryAllocationPercentage not set" -}}
        {{- end -}}
        {{- if and (index .Values "resources" "eric-oss-auto-config-consistency" "jvm" "smallMemoryAllocationMaxPercentage") (index .Values "resources" "eric-oss-auto-config-consistency" "jvm" "largeMemoryAllocationMaxPercentage") -}}
            {{- if lt $ramLimit $ramComparison -}}
                {{- $maxRAM = (index .Values "resources" "eric-oss-auto-config-consistency" "jvm" "smallMemoryAllocationMaxPercentage") | int -}}
                {{- $maxRAM = printf "-XX:MaxRAMPercentage=%d" $maxRAM -}}
            {{- else -}}
                {{- $maxRAM = (index .Values "resources" "eric-oss-auto-config-consistency" "jvm" "largeMemoryAllocationMaxPercentage") | int -}}
                {{- $maxRAM = printf "-XX:MaxRAMPercentage=%d" $maxRAM -}}
            {{- end -}}
        {{- else -}}
            {{- fail "smallMemoryAllocationMaxPercentage | largeMemoryAllocationMaxPercentage not set" -}}
        {{- end -}}
    {{- else -}}
        {{- fail "jvm heap percentages are not set" -}}
    {{- end -}}

{{- printf "%s %s %s %s %s" $initRAM $maxRAM $heapDump $heapDumpPath $exitOnError -}}
{{- end -}}

{{/*
DR-D470222-010 : global.log.streamingMethod shall be used to determine logging method: indirect/direct/dual
*/}}
{{- define "eric-oss-auto-config-consistency.streamingMethod" -}}
  {{- $streamingMethod := "indirect" -}}
  {{- if ((((.Values).global).log).streamingMethod) -}}
    {{- $streamingMethod = (((.Values).global).log).streamingMethod -}}
  {{- end -}}
  {{- if (((.Values).log).streamingMethod) -}}
    {{- $streamingMethod = ((.Values).log).streamingMethod -}}
  {{- end -}}
  {{- printf "%s" $streamingMethod -}}
{{ end }}

{{/*
Define the label needed for reaching eric-log-transformer (DR-470222-010)
*/}}
{{- define "eric-oss-auto-config-consistency.directStreamingLabel" -}}
{{- $streamingMethod := (include "eric-oss-auto-config-consistency.streamingMethod" .) -}}
{{- if or (eq "direct" $streamingMethod) (eq "dual" $streamingMethod) }}
logger-communication-type: "direct"
{{- end -}}
{{- end -}}

{{/*
Define the label needed for scraping pm-metrics
*/}}
{{- define "eric-oss-auto-config-consistency.pmMetricsScraping" -}}
service.cleartext/scraping: "true"
{{- end -}}

{{/*
Define logging environment variables (DR-470222-010)
*/}}
{{ define "eric-oss-auto-config-consistency.loggingEnv" }}
{{- $streamingMethod := (include "eric-oss-auto-config-consistency.streamingMethod" .) -}}
{{- if or (eq "direct" $streamingMethod) (eq "dual" $streamingMethod) }}
  {{- if eq "direct" $streamingMethod }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-https.xml"
- name: LOG_STREAMING_METHOD
  value: "direct"
  {{- end }}
  {{- if eq "dual" $streamingMethod }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-dual-sec.xml"
- name: LOG_STREAMING_METHOD
  value: "dual"
  {{- end }}
- name: POD_NAME
  valueFrom:
    fieldRef:
      fieldPath: metadata.name
- name: POD_UID
  valueFrom:
    fieldRef:
      fieldPath: metadata.uid
- name: CONTAINER_NAME
  value: eric-oss-auto-config-consistency
- name: NODE_NAME
  valueFrom:
    fieldRef:
      fieldPath: spec.nodeName
- name: NAMESPACE
  valueFrom:
    fieldRef:
      fieldPath: metadata.namespace
{{- else if eq $streamingMethod "indirect" }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-json.xml"
- name: LOG_STREAMING_METHOD
  value: "indirect"
{{- else }}
  {{- fail ".log.streamingMethod unknown" }}
{{- end -}}
{{- end -}}

{{/*
IAM URL
*/}}
{{ define "eric-oss-auto-config-consistency.iamUrl" }}
  {{- $iamHost := .Values.baseUrl -}}
  {{- if ((((.Values).global).hosts).iam) -}}
    {{- $iamHost = .Values.global.hosts.iam -}}
  {{- end -}}
  {{- if (((.Values).iam).host) -}}
    {{- $iamHost = .Values.iam.host -}}
  {{- end -}}
  {{ if hasPrefix "https://" $iamHost }}
    {{- printf "%s" $iamHost -}}
  {{ else }}
    {{- printf "https://%s" $iamHost -}}
  {{- end -}}
{{- end -}}

{{/*
NCMP Hostname
Default to global GAS hostname, but prefer NCMP hostname if provided
*/}}
{{ define "eric-oss-auto-config-consistency.ncmpHost" }}
  {{- $ncmpHost := "" -}}
  {{- if ((((.Values).global).hosts).gas) -}}
    {{- $ncmpHost = .Values.global.hosts.gas -}}
  {{- end -}}
  {{- if (((.Values).ncmp).host) -}}
    {{- $ncmpHost = .Values.ncmp.host -}}
  {{- end -}}
  {{- printf "%s" $ncmpHost -}}
{{- end -}}

{{/*
NCMP URL
*/}}
{{ define "eric-oss-auto-config-consistency.ncmpUrl" }}
  {{- $ncmpUrl := (include "eric-oss-auto-config-consistency.ncmpHost" . ) -}}
  {{ if empty $ncmpUrl }}
    {{- print "" -}}
  {{ else if hasPrefix "https://" $ncmpUrl }}
    {{- printf "%s" $ncmpUrl -}}
  {{ else }}
    {{- printf "https://%s" $ncmpUrl -}}
  {{- end -}}
{{- end -}}

{{/*
TLS version
*/}}
{{ define "eric-oss-auto-config-consistency.clientProtocol" }}
  {{- $clientProtocol := .Values.tls.clientProtocol -}}
  {{- printf "-Djdk.tls.client.protocols=%s" $clientProtocol -}}
{{- end -}}

{{/*

Merge labels with commons
*/}}
{{- define "eric-oss-auto-config-consistency.labels" -}}
    {{- $directStreamingLabel := include "eric-oss-auto-config-consistency.directStreamingLabel" . | fromYaml -}}
    {{- $common := include "eric-oss-auto-config-consistency.common-labels" . | fromYaml -}}
    {{- $config := include "eric-oss-auto-config-consistency.config-labels" . | fromYaml -}}
    {{- include "eric-oss-auto-config-consistency.mergeLabels" (dict "location" .Template.Name "sources" (list $directStreamingLabel $common $config)) | trim }}
{{- end -}}

{{/*

Frontend labels
*/}}
{{- define "eric-oss-auto-config-consistency-frontend.labels" -}}
    {{- $frontendLabels := .Values.frontend.labels -}}
    {{- $commonLabels := include "eric-oss-auto-config-consistency.labels" . | fromYaml -}}
    {{- include "eric-oss-auto-config-consistency.mergeLabels" (dict "location" .Template.Name "sources" (list $commonLabels $frontendLabels)) | trim -}}
{{- end -}}

{{/*
Merge annotations with commons
*/}}
{{- define "eric-oss-auto-config-consistency.annotations" -}}
    {{- $prometheus := include "eric-oss-auto-config-consistency.prometheus" . | fromYaml -}}
    {{- $productInfo := include "eric-oss-auto-config-consistency.product-info" . | fromYaml -}}
    {{- $config := include "eric-oss-auto-config-consistency.config-annotations" . | fromYaml -}}
    {{- include "eric-oss-auto-config-consistency.mergeAnnotations" (dict "location" .Template.Name "sources" (list $prometheus $productInfo $config)) | trim }}
{{- end -}}

{{/*

Frontend Annotations
*/}}
{{- define "eric-oss-auto-config-consistency-frontend.annotations" -}}
    {{- $frontendAnnotations := .Values.frontend.annotations -}}
    {{- $commonAnnotations := include "eric-oss-auto-config-consistency.annotations" . | fromYaml -}}
    {{- include "eric-oss-auto-config-consistency.mergeAnnotations" (dict "location" .Template.Name "sources" (list $commonAnnotations $frontendAnnotations)) | trim -}}
{{- end -}}

{{/*
Merge security-annotations with commons
*/}}
{{- define "eric-oss-auto-config-consistency.securityAnnotations" -}}
    {{- $securityPolicy := include "eric-oss-auto-config-consistency.securityPolicy.annotations" . | fromYaml -}}
    {{- $common := include "eric-oss-auto-config-consistency.annotations" . | fromYaml -}}
    {{- include "eric-oss-auto-config-consistency.mergeAnnotations" (dict "location" .Template.Name "sources" (list $common $securityPolicy)) | trim }}
{{- end -}}

{{/*
Create container level annotations
*/}}
{{- define "eric-oss-auto-config-consistency.container-annotations" -}}
    {{- $appArmorAnnotations := include "eric-oss-auto-config-consistency.AppArmor-annotations" . | fromYaml -}}
    {{- $commonAnnotations := include "eric-oss-auto-config-consistency.annotations" . | fromYaml -}}
    {{- include "eric-oss-auto-config-consistency.mergeAnnotations" (dict "location" .Template.Name "sources" (list $commonAnnotations $appArmorAnnotations )) | trim }}
{{- end -}}

{{/*
Label to access EACC Database
*/}}
{{ define "eric-oss-auto-config-consistency.pgAccessLabel" }}
  {{- $eaccPgValues := (index .Values "eric-oss-auto-config-consistency-pg") -}}
  {{- if $eaccPgValues.enabled -}}
    {{- $eaccPgName := "eric-oss-auto-config-consistency-pg" -}}
    {{- if (($eaccPgValues).nameOverride) -}}
      {{- $eaccPgName = $eaccPgValues.nameOverride -}}
    {{- end -}}
    {{ printf "%s-access" $eaccPgName -}}: "true"
  {{- end -}}
{{- end -}}