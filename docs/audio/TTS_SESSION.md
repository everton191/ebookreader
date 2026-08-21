# Sessão TTS persistente

`EnginePlayer` é o proprietário da sessão. A chave de reutilização contém voz,
engine, quantidade de instâncias e threads. Se a chave não muda, `loadModel` é
ignorado e a mesma sessão ONNX atende os capítulos seguintes.

Estados equivalentes:

- `UNLOADED`: sem `loadedVoiceId`;
- `LOADING`: `loadAndPlay` dentro do carregamento protegido por `EngineMutex`;
- `WARMING_UP`: `EngineState.Warming` até o primeiro PCM;
- `READY/ACTIVE`: modelo carregado e pipeline disponível/tocando;
- `FAILED`: `PlaybackError` após falha de carga/síntese;
- `RELEASING`: encerramento do pipeline/pool durante troca incompatível.

Não existe criação de sessão ONNX por segmento. O `EngineStreamingSource`
chama repetidamente o handle carregado; `EngineMutex` impede destruição durante
JNI. Uma classe paralela chamada `PersistentTtsSession` não foi criada porque
duplicaria esse proprietário e introduziria duas fontes de verdade.
