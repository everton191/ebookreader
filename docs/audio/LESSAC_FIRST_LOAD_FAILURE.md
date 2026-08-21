# Falha no primeiro carregamento da Lessac

## Evidência do baseline

No Zenfone 8, a Lessac foi escolhida no onboarding. Ao abrir o primeiro EPUB,
o app permaneceu em `Loading voice + chapter text` e terminou em
`Couldn't load this chapter`. Após selecionar manualmente uma voz System TTS
offline, o capítulo 3 reproduziu por AudioTrack e MediaSession.

## Fluxo encontrado no código

1. `VoicePickerGateViewModel.pick()` coleta `VoiceManager.download()`.
2. O Piper grava diretamente `model.onnx` e `tokens.txt` no diretório final.
3. `markInstalled()` e `setActive()` rodam depois que as duas transferências
   terminam.
4. Antes do JNI, `EnginePlayer` chama `modelFilesPresent()`, que valida apenas
   a existência de ambos os arquivos.
5. A carga nativa ocorre em `VoiceEngine.loadModel()`.

## Lacunas encontradas

- O catálogo declara URL e tamanho aproximado, mas não SHA-256 esperado.
- Não há manifesto de integridade da voz.
- Não há instalação atômica por diretório temporário.
- `modelFilesPresent()` não verifica tamanho mínimo nem hash.
- O baseline anterior não capturava caminho, tamanho, hash e duração da carga
  em um bloco de diagnóstico específico da Lessac.

Portanto, um arquivo parcial/corrompido pode existir e chegar ao carregador
nativo. A execução observada não permite afirmar definitivamente que esta foi
a causa, pois o app release não permite `run-as` para inspecionar os arquivos
privados depois do fato.

## Instrumentação desta fase

O carregamento Piper da Lessac passa a registrar:

- `LESSAC_MODEL_PATH`
- `LESSAC_MODEL_EXISTS`
- `LESSAC_MODEL_SIZE`
- `LESSAC_CHECKSUM`
- `LESSAC_LOAD_START`
- `LESSAC_LOAD_END`
- `LESSAC_LOAD_MS`
- `LESSAC_EXCEPTION`
- `LESSAC_FALLBACK`

Sem hash esperado publicado no catálogo, `LESSAC_CHECKSUM` é o SHA-256
observado, não uma confirmação de validade. A correção estrutural recomendada
para fase própria é manifesto versionado + `.part` + hash + rename atômico.

## Estado

- Causa confirmada: não.
- Hipótese principal: modelo não pronto ou não íntegro apesar do estado ativo.
- Race condition comprovada: não.
- Falha ONNX/runtime comprovada: não.
- Fallback System TTS: aprovado no baseline.
