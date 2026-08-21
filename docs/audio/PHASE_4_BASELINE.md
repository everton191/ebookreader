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
- `RAM_MB` por amostra de síntese;
- contadores preparados para underrun, falha e fallback;
- pontos para `TTS_WARMUP_MS` e `PLAY_TO_FIRST_AUDIO_MS`.

O RTF é calculado como `generationMs / audioDurationMs`, com PCM mono de 16
bits e sample rate efetivo da engine.

## Baseline quantitativo

Ainda não preenchido: o telefone permanece bloqueado por autenticação e o ADB
não pode iniciar/importar/tocar o livro. Nenhum número será estimado ou marcado
como medido sem logcat real.

| Métrica | Antes | Evidência |
|---|---:|---|
| MODEL_LOAD_MS | pendente | `Phase4Tts`/logcat |
| TTS_WARMUP_MS | pendente | `Phase4Tts`/logcat |
| PLAY_TO_FIRST_AUDIO_MS | pendente | `Phase4Tts`/logcat |
| RTF por segmento | pendente | `Phase4Tts`/logcat |
| READY_AUDIO_SECONDS | pendente | `Phase4Tts`/logcat |
| CACHE_HIT_RATE | pendente | `Phase4Tts`/logcat |
| RAM_MB | pendente | logcat + `dumpsys meminfo` |
| QUEUE_UNDERRUN_COUNT | pendente | `Phase4Tts`/logcat |
| TTS_FAILURE_COUNT | pendente | `Phase4Tts`/logcat |
| FALLBACK_COUNT | pendente | `Phase4Tts`/logcat |

## Decisão de engine

Nenhuma engine foi trocada. Lessac/Piper e System TTS permanecem as engines
reais a medir. Supertonic só poderá entrar na comparação se estiver instalado
e funcional nesta base; o relatório não presumirá disponibilidade.
