# Fase 4 — baseline do pipeline TTS

Data: 2026-08-21  
Dispositivo-alvo: Zenfone 8 (`ASUS_I005DA`, Android 13)

## Arquitetura encontrada

O Candela já possui parte substancial do desenho pedido e ela será evoluída,
não duplicada:

- `EnginePlayer` mantém o modelo ativo entre capítulos e ignora `loadModel`
  quando voz, engine e configuração paralela não mudaram;
- `EngineStreamingSource` possui produtor dedicado, fila limitada e estados
  implícitos de geração/fila/reprodução;
- o headroom é medido em milissegundos de PCM, embora a capacidade ainda seja
  configurada principalmente por quantidade de chunks;
- há hysteresis de underrun em 7 s e retomada em 10 s, além de prebuffer inicial;
- `PcmCache` oferece cache persistente, lease de escritor único, integridade,
  quota e eviction;
- cache hit usa `CacheFileSource` e seek pode reutilizar o PCM/index existente;
- `EngineMutex` impede troca/destruição do modelo durante JNI;
- síntese paralela já existe e é configurável, com custo de uma sessão ONNX por
  worker;
- capítulo seguinte já tem prefetch de texto; a confirmação de PCM antecipado
  ainda faz parte do gate no aparelho.

## Instrumentação adicionada

A tag `Phase4Tts` registra:

- `MODEL_LOAD_MS` e se a sessão foi reutilizada;
- `SEGMENT_GENERATION_MS`;
- `SEGMENT_AUDIO_DURATION_MS`;
- `REAL_TIME_FACTOR`;
- engine, voz, preset e tamanho do segmento;
- `READY_AUDIO_SECONDS`;
- `CACHE_HIT_RATE`, hits e misses;
- `RAM_CACHE_HIT_RATE`, entries e bytes;
- `RAM_MB` por amostra de síntese;
- contadores preparados para underrun, falha e fallback;
- pontos para `TTS_WARMUP_MS` e `PLAY_TO_FIRST_AUDIO_MS`.

As chamadas de métrica são best effort: falha de `Log` ou leitura de PSS não
pode encerrar o produtor de áudio.

O RTF é calculado como `generationMs / audioDurationMs`, com PCM mono de 16
bits e sample rate efetivo da engine.

## Baseline quantitativo

Medido em build debug no Zenfone 8, Lessac Piper low, 1 worker, 1,0×, capítulo
2 de *Pride and Prejudice* importado como EPUB novo. O build debug preserva os
logs e tem overhead; o gate final será repetido em variante profileable.

| Métrica | Antes | Evidência |
|---|---:|---|
| MODEL_LOAD_MS | 2.405 ms | Lessac, primeira carga |
| TTS_WARMUP_MS | não capturado | abertura direta do capítulo |
| PLAY_TO_FIRST_AUDIO_MS | 5.992 ms | cache miss/carga fria |
| RTF por segmento | 0,11–0,31 | 17 amostras iniciais; maioria 0,15–0,21 |
| READY_AUDIO_SECONDS | 3,18–111,58 s | provou excesso do controle por chunks |
| CACHE_HIT_RATE | 0% | primeiro play, 1 miss |
| RAM_MB | 739→766 MB PSS | antes/depois de 60 s, build debug |
| QUEUE_UNDERRUN_COUNT | 1 | underrun de partida |
| TTS_FAILURE_COUNT | pendente | `Phase4Tts`/logcat |
| FALLBACK_COUNT | 0 observado | janela inicial |

A Lessac carregou com 63.149.198 bytes, SHA-256 observado
`8177731223c47c72764e7021866f549dc72b894abc6b252c7f1ca0f8f65ad659` e
`loadModel=Success`.

Com a tela apagada por 60 s, a MediaSession permaneceu ativa em estado 3 e a
posição avançou de 41.920 para 87.360 ms. Não houve crash ou ANR nessa janela.

## Decisão de engine

Nenhuma engine foi trocada. Lessac/Piper e System TTS permanecem as engines
reais a medir. Supertonic só poderá entrar na comparação se estiver instalado
e funcional nesta base; o relatório não presumirá disponibilidade.
