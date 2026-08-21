# Relatório da Fase 2 — redução segura do Candela

Data: 2026-08-21  
Branch: `foundation/candela-clean`  
Baseline: `2422ccf1`

## Resultado

A redução foi feita em lotes pequenos, preservando o núcleo de leitura e
playback. Nove módulos-folha deixaram de ser empacotados e foram removidos do
repositório. Os contratos de `core-llm` continuam compilando, mas nenhum dos
provedores atuais de rede pode ser ativado; as entradas de IA e de vozes cloud
também saíram do hub de configurações.

## Módulos removidos

- `source-arxiv`
- `source-calendar`
- `source-epic-free-games`
- `source-google-news`
- `source-hackernews`
- `source-librivox`
- `source-plos`
- `source-readability`
- `source-wikisource`

Esses módulos foram escolhidos porque não tinham imports Kotlin fora do próprio
módulo. Primeiro suas dependências foram retiradas de `:app`, o APK foi
recompilado, e só depois os includes e diretórios foram removidos.

## Mantido nesta fase

- `app`, `feature`, `core-data`, `core-playback`, `core-ui`;
- `source-epub`, `source-pdf`, `source-google-drive`;
- `source-ocr`, handbook e writers, por ainda participarem de contratos/UI;
- `source-azure`, porque `core-playback` ainda possui acoplamento direto com o
  engine e removê-lo sem uma nova abstração seria uma refatoração de alto risco;
- integrações com configurações concretas em `app` (GitHub, Reddit, chats,
  Notion, RSS e outras). Elas são candidatas da próxima rodada, mas removê-las
  neste lote exigiria reescrever `SettingsRepositoryUi`, DI e navegação.

Manter esses itens acoplados foi uma decisão conservadora: a Fase 2 não autoriza
uma refatoração ampla do playback ou do repositório de configurações.

## IA e cloud TTS

- `LlmRepository` mantém interfaces e tipos para um futuro backend local.
- Os provedores atuais ficam fail-closed com `NotConfigured`.
- O hub não mostra AI, sessões de AI nem Cloud Voices.
- A implementação Azure ainda compila por dependência do playback, mas não tem
  ponto de entrada no hub principal.

## Diagnóstico Lessac

O primeiro carregamento agora registra `LESSAC_MODEL_PATH`, existência, tamanho,
SHA-256 real, início, fim, duração, exceção e necessidade de fallback. O fluxo
de download ainda grava no destino final sem `.part`, checksum esperado ou
rename atômico; portanto, a hipótese principal continua sendo arquivo parcial
ou inválido aceito como instalado. A evidência e os próximos passos estão em
`docs/audio/LESSAC_FIRST_LOAD_FAILURE.md`.

## Comparativo medido

| Métrica | Baseline | Fase 2 | Diferença |
|---|---:|---:|---:|
| APK release | 192.131.699 B | 192.000.359 B | -131.340 B (-0,07%) |
| Cold start no Zenfone 8 | 1.100 ms | 790 ms | -310 ms (-28,2%) |
| Módulos Gradle | 48 | 39 | -9 |

O ganho pequeno de APK é coerente com R8: a maior parte do código dos módulos
sem consumidores já era removida no release. O valor estrutural é reduzir o
grafo, o catálogo e a superfície de manutenção sem tocar nas bibliotecas
nativas de TTS responsáveis pela maior parte do APK.

## Validação executada

- `:core-playback:compileReleaseKotlin`: sucesso;
- build intermediário depois de desabilitar os nove módulos: sucesso;
- `:app:assembleRelease` depois de remover os módulos: sucesso;
- `:app:compileReleaseKotlin` depois de desabilitar cloud: sucesso;
- build release final: sucesso;
- instalação com `adb install -r`: sucesso;
- cold start real: `LaunchState: COLD`, 790 ms;
- EPUB existente aberto com título e capítulo preservados;
- reprodução: MediaSession `state=3`, AudioTrack 24 kHz;
- tela apagada: posição avançou de 97.415 para 103.520 ms;
- pausa por MediaSession: `state=2`;
- reinstalação do APK final e nova retomada: `state=3` em 138.800 ms.

## Riscos e validação manual pendente

- A Lessac precisa de uma nova instalação limpa da voz para capturar os logs da
  primeira carga e confirmar a hipótese; a instrumentação não equivale à cura.
- Conferir manualmente no aparelho que nenhuma rota profunda antiga reabre as
  telas cloud, embora o hub já não as exponha e o repositório de LLM falhe
  fechado.
- Importar um EPUB novo pelo seletor de arquivos. A regressão foi validada com o
  EPUB já importado do baseline, sem apagar os dados do usuário.
- Google Drive foi preservado, mas não houve nova autenticação/download nesta
  rodada para evitar alterar credenciais do aparelho.

