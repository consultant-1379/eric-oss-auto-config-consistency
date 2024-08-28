{{- define "eric-oss-auto-config-consistency.mainImagePath" -}}
    {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
    {{- $registryUrl := (index $productInfo "images" "eric-oss-auto-config-consistency" "registry") -}}
    {{- $repoPath := (index $productInfo "images" "eric-oss-auto-config-consistency" "repoPath") -}}
    {{- $name := (index $productInfo "images" "eric-oss-auto-config-consistency" "name") -}}
    {{- $tag := (index $productInfo "images" "eric-oss-auto-config-consistency" "tag") -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.url -}}
                {{- $registryUrl = .Values.global.registry.url -}}
            {{- end -}}
            {{- if not (kindIs "invalid" .Values.global.registry.repoPath) -}}
                {{- $repoPath = .Values.global.registry.repoPath -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
            {{- $repoPath = .Values.imageCredentials.repoPath -}}
        {{- end -}}
        {{- if (index .Values "imageCredentials" "eric-oss-auto-config-consistency") -}}
            {{- if (index .Values "imageCredentials" "eric-oss-auto-config-consistency" "registry") -}}
                {{- if (index .Values "imageCredentials" "eric-oss-auto-config-consistency" "registry" "url") -}}
                    {{- $registryUrl = (index .Values "imageCredentials" "eric-oss-auto-config-consistency" "registry" "url") -}}
                {{- end -}}
            {{- end -}}
            {{- if not (kindIs "invalid" (index .Values "imageCredentials" "eric-oss-auto-config-consistency" "repoPath")) -}}
                {{- $repoPath = (index .Values "imageCredentials" "eric-oss-auto-config-consistency" "repoPath") -}}
            {{- end -}}
            {{- if not (kindIs "invalid" (index .Values "imageCredentials" "eric-oss-auto-config-consistency" "tag")) -}}
              {{- $tag = index .Values "imageCredentials" "eric-oss-auto-config-consistency" "tag" -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if $repoPath -}}
        {{- $repoPath = printf "%s/" $repoPath -}}
    {{- end -}}
    {{- printf "%s/%s%s:%s" $registryUrl $repoPath $name $tag -}}
{{- end -}}

{{- define "eric-oss-auto-config-consistency-ui.mainImagePath" -}}
    {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
    {{- $registryUrl := (index $productInfo "images" "eric-oss-auto-config-consistency-ui" "registry") -}}
    {{- $repoPath := (index $productInfo "images" "eric-oss-auto-config-consistency-ui" "repoPath") -}}
    {{- $name := (index $productInfo "images" "eric-oss-auto-config-consistency-ui" "name") -}}
    {{- $tag := (index $productInfo "images" "eric-oss-auto-config-consistency-ui" "tag") -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.url -}}
                {{- $registryUrl = .Values.global.registry.url -}}
            {{- end -}}
            {{- if not (kindIs "invalid" .Values.global.registry.repoPath) -}}
                {{- $repoPath = .Values.global.registry.repoPath -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
            {{- $repoPath = .Values.imageCredentials.repoPath -}}
        {{- end -}}
        {{- if (index .Values "imageCredentials" "eric-oss-auto-config-consistency-ui") -}}
            {{- if (index .Values "imageCredentials" "eric-oss-auto-config-consistency-ui" "registry") -}}
                {{- if (index .Values "imageCredentials" "eric-oss-auto-config-consistency-ui" "registry" "url") -}}
                    {{- $registryUrl = (index .Values "imageCredentials" "eric-oss-auto-config-consistency-ui" "registry" "url") -}}
                {{- end -}}
            {{- end -}}
            {{- if not (kindIs "invalid" (index .Values "imageCredentials" "eric-oss-auto-config-consistency-ui" "repoPath")) -}}
                {{- $repoPath = (index .Values "imageCredentials" "eric-oss-auto-config-consistency-ui" "repoPath") -}}
            {{- end -}}
            {{- if not (kindIs "invalid" (index .Values "imageCredentials" "eric-oss-auto-config-consistency-ui" "tag")) -}}
              {{- $tag = index .Values "imageCredentials" "eric-oss-auto-config-consistency-ui" "tag" -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if $repoPath -}}
        {{- $repoPath = printf "%s/" $repoPath -}}
    {{- end -}}
    {{- printf "%s/%s%s:%s" $registryUrl $repoPath $name $tag -}}
{{- end -}}

