# Auditoria de preparação para PT-BR

Esta fase não traduz a interface. O objetivo é separar texto necessário de
texto pertencente às integrações removidas.

## Estado atual

- Recursos Android existem principalmente em `feature/src/main/res/values/strings.xml`.
- Há centenas de textos hardcoded em Composables e mapeamentos Kotlin.
- Não existe cobertura completa em `values-pt-rBR`.
- O app original mistura inglês de produto, nomes técnicos, conteúdo de
  providers e algumas mensagens fornecidas pelo Android em português.

## Prioridades da Fase 3

1. Biblioteca, importação de arquivo, reader, player e mini-player.
2. Vozes, download, erros e fallback TTS.
3. Configurações mantidas e Google Drive.
4. Diagnóstico avançado.

## Classificação

- **Remover agora:** taglines, autenticação e settings de sources retiradas.
- **Migrar para resources:** `Text("...")`, `contentDescription = "..."`,
  labels e mensagens de erro mantidas.
- **Não traduzir:** ids, nomes de modelos/engines, métricas e logs.
- **Traduzir em PT-BR:** toda string visível ao usuário comum.

## Riscos

- `SettingsScreen.kt`, `UiContracts.kt` e `AudiobookView.kt` são arquivos
  grandes; a extração deve ser incremental para não misturar tradução com
  refatoração funcional.
- Strings de módulos removidos não devem ser traduzidas nem mantidas.
- Conteúdo EPUB/PDF e nomes de vozes não pertencem à localização do app.
