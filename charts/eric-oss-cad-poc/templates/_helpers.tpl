{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-cad-poc.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
App labels for cad optimization
*/}}
{{- define "eric-oss-cad-poc.labels" -}}
app.kubernetes.io/name: {{ include "eric-oss-cad-poc.name" . }}
{{ include "eric-oss-cad-poc.common-labels" . }}
{{- end -}}

{{/*
App labels for cad optimization
*/}}
{{- define "eric-oss-cad-poc.hooks-labels" -}}
app.kubernetes.io/name: {{ include "eric-oss-cad-poc.name" . }}-hook
{{ include "eric-oss-cad-poc.common-labels" . }}
{{- end -}}

{{/*
DB labels for cad optimization
*/}}
{{- define "eric-oss-cad-poc.db-labels" -}}
app.kubernetes.io/name: {{ include "eric-oss-cad-poc.name" . }}-db
{{ include "eric-oss-cad-poc.common-labels" . }}
{{- end -}}

{{/*
Common labels for cad optimization
*/}}
{{- define "eric-oss-cad-poc.common-labels" -}}
helm.sh/chart: {{ include "eric-oss-cad-poc.chart" . }}
app.kubernetes.io/version: {{ include "eric-oss-cad-poc.version" . }}
app.kubernetes.io/instance: {{ .Release.Name | quote }}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "eric-oss-cad-poc.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-oss-cad-poc.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "eric-oss-cad-poc.selectorLabels" -}}
app.kubernetes.io/name: {{ include "eric-ran-rapp-cad.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
cad optimization label name
*/}}
{{- define "eric-oss-cad-poc.labelName" -}}
app.kubernetes.io/name: {{ .Values.container.label }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "eric-oss-cad-poc.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "eric-ran-rapp-cad.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Get product info
*/}}
{{- define "eric-oss-cad-poc.product-info" -}}
ericsson.com/product-name: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productName | quote }}
ericsson.com/product-number: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productNumber | quote }}
ericsson.com/product-revision: {{regexReplaceAll "(.*)[+|-].*" .Chart.Version "${1}" | quote }}
{{- end }}

{{- define "eric-oss-cad-poc.version" -}}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Timezone variable
*/}}
{{- define "eric-oss-cad-poc.timezone" -}}
{{- $timezone := "UTC" -}}
{{- if .Values.global  -}}
    {{- if .Values.global.timezone -}}
        {{- $timezone = .Values.global.timezone -}}
    {{- end -}}
{{- end -}}
{{- print $timezone | quote -}}
{{- end -}}

{{/*
Create image pull secrets
*/}}
{{- define "eric-oss-cad-poc.registryImagePullPolicy" -}}
  {{- $pullSecret := "" -}}
  {{- if .Values.global -}}
      {{- if .Values.global.pullSecret -}}
          {{- $pullSecret = .Values.global.pullSecret -}}
      {{- end -}}
  {{- end -}}
  {{- if .Values.imageCredentials -}}
      {{- if .Values.imageCredentials.pullSecret -}}
          {{- $pullSecret = .Values.imageCredentials.pullSecret -}}
      {{- end -}}
  {{- end -}}
  {{- print $pullSecret -}}
{{- end -}}

{{/*
The image path (DR-D1121-067)(DR-D1121-106)
*/}}
{{- define "eric-oss-cad-poc.image" -}}
  {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
  {{- $productInfo = index $productInfo "images" .imageName -}}
  {{- $global := .Values.global -}}
  {{- if $global.registry -}}
      {{- if $global.registry.url -}}
        {{- $_ := set $productInfo "registry" $global.registry.url -}}
      {{- end -}}
      {{- if not (kindIs "invalid" $global.registry.repoPath) -}}
        {{- $_ := set $productInfo "repoPath" ($global.registry.repoPath) -}}
      {{- end -}}
  {{- end -}}
  {{- if .Values.imageCredentials -}}
    {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
      {{- $_ := set $productInfo "repoPath" (index .Values "imageCredentials" "repoPath") }}
    {{- end -}}
    {{- if hasKey .Values.imageCredentials .imageName -}}
        {{- if hasKey (index .Values "imageCredentials" .imageName) "registry" }}
          {{- if hasKey (index .Values "imageCredentials" .imageName "registry") "url" }}
            {{- $_ := set $productInfo "registry" (index .Values "imageCredentials" .imageName "registry" "url") }}
          {{- end }}
        {{- end }}
        {{- if not (kindIs "invalid" (index .Values "imageCredentials" .imageName "repoPath") ) -}}
          {{- $_ := set $productInfo "repoPath" (index .Values "imageCredentials" .imageName "repoPath") }}
        {{- end -}}
    {{- end }}
  {{- end -}}
  {{- if hasKey (index .Values "images" .imageName) "name" -}}
    {{- $_ := set $productInfo "name" (index .Values "images" .imageName "name") -}}
  {{- end -}}
  {{- if hasKey (index .Values "images" .imageName) "tag" -}}
    {{- $_ := set $productInfo "tag" (index .Values "images" .imageName "tag") -}}
  {{- end -}}
  {{- if $productInfo.repoPath -}}
      {{- $_ := set $productInfo "repoPath" (printf "%s/" $productInfo.repoPath) -}}
  {{- end -}}
  {{- printf "%s/%s%s:%s" $productInfo.registry $productInfo.repoPath $productInfo.name $productInfo.tag -}}
{{- end -}}

{{/*
Create image pull secrets
*/}}
{{- define "eric-oss-cad-poc.pullSecrets" -}}
  {{- $pullSecret := "" -}}
  {{- if .Values.global -}}
      {{- if .Values.global.pullSecret -}}
          {{- $pullSecret = .Values.global.pullSecret -}}
      {{- end -}}
  {{- end -}}
  {{- print $pullSecret -}}
{{- end -}}

{{/*
Define the role reference for security policy
*/}}
{{- define "eric-oss-cad-poc.securityPolicy.reference" -}}
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
Get image pull secret
*/}}
{{- define "eric-oss-cad-poc.pullSecret" -}}
  {{- $pullSecret := "" -}}
  {{- if .Values.global -}}
      {{- if .Values.global.pullSecret -}}
          {{- $pullSecret = .Values.global.pullSecret -}}
      {{- end -}}
  {{- end -}}
  {{- if .Values.imageCredentials -}}
      {{- if .Values.imageCredentials.pullSecret -}}
          {{- $pullSecret = .Values.imageCredentials.pullSecret -}}
      {{- end -}}
  {{- end -}}
  {{- print $pullSecret -}}
{{- end -}}