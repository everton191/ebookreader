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

## Preparação antecipada

Ao abrir o leitor, `HybridReaderScreen` solicita `prewarmEngine()`. O binding de
UI mantém uma conexão de serviço com `StoryvoxPlaybackService` usando o contexto
da aplicação e entrega a solicitação ao `PlaybackController`, inclusive quando
o player só fica disponível depois do pedido. Esse binding não inicia áudio nem
notificação por conta própria.

`EnginePlayer.prewarm()` carrega a voz selecionada sob o mesmo `EngineMutex` do
Play e reaproveita a sessão quando a configuração não mudou. Chamadas repetidas
são coalescidas. Após uma carga local real, roda silenciosamente um benchmark de
cinco frases PT-BR e persiste load, warm-up, RTF e PSS por voz, versão do modelo
e preset. Azure e System TTS não são acionados nesse benchmark antecipado para
evitar rede, custo ou fala fora de uma ação explícita do usuário.

O benchmark não segura o mutex do Play: ele verifica a prioridade entre frases
e para assim que um capítulo começa a ser solicitado. O `EngineMutex` limita a
espera do Play, no pior caso, à única síntese nativa que já estava em andamento.
