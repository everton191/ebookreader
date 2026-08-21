# Implementação PT-BR

Foi criado `values-pt-rBR/strings.xml` com 152 recursos para os fluxos
principais: biblioteca, importação, leitura, player, mini-player, vozes,
downloads, aparência, configurações e mensagens de erro/fallback.

Nomes próprios de engines, modelos e arquivos permanecem no idioma original.
Conteúdo de EPUB/PDF não é traduzido pelo aplicativo.

## Dívida conhecida

A base ainda possui textos literais em telas avançadas e código legado que
ficou inacessível pela interface enxuta. A busca estática encontrou 31
ocorrências de `Text("...")` somente em Library/Settings; nem todas são
visíveis no caminho principal. A extração restante deve ser incremental.

Compilação Kotlin de `:feature` e APK release foram aprovados. A inspeção
visual PT-BR no Zenfone depende do desbloqueio do aparelho.
