# Relatório da Fase 5 — Gemma local e dublagem

Status em 22/08/2026: **em andamento; não aprovada para entrega no aparelho**.

## Implementado no código

- Download local opt-in do Gemma 4 E2B com arquivo parcial, tamanho e SHA-256.
- Runtime LiteRT-LM local, sem fallback para API de IA em rede.
- `NarrationPlan` persistente por segmento, com texto, versão de schema e versão do modelo.
- `CharacterBible` persistente; falantes com confiança suficiente são registrados de forma conservadora.
- Ao iniciar a reprodução, a análise é agendada em segundo plano somente se o Gemma estiver instalado e habilitado. O player nunca a aguarda.
- O `PlaybackResourceGovernor` controla quando a análise pode iniciar.
- Para vozes Kokoro, um plano já persistido pode aplicar speaker, velocidade e tom por segmento sem recarregar o modelo. Planos ativos usam síntese serial e não reutilizam PCM/RAM cache, evitando áudio de uma dublagem anterior.
- Piper permanece no narrador carregado: trocar o modelo por frase não é permitido, pois reduziria a fila de áudio.

## Evidência de build e testes

- `:app:compileDebugKotlin` passou após o agendamento de análise.
- `:core-llm:testDebugUnitTest --tests=in.jphe.storyvox.llm.narration.NarrationPlanStoreTest` passou.
- `:core-playback:testDebugUnitTest --tests=in.jphe.storyvox.playback.tts.source.EngineStreamingSourceTest` passou.

## Ainda não comprovado

- Modelo de 2,59 GB baixado, verificado, carregado e respondendo no Zenfone 8.
- JSON real produzido pelo Gemma em português brasileiro.
- Troca audível de speaker Kokoro em um capítulo real.
- Tela "Personagens e dublagem" e seleção manual acessível ao usuário.
- Reanálise manual por capítulo acessível na interface.
- Reprodução longa, transição de capítulos, tela apagada, MediaSession e RAM com Gemma ativo.
- Gate da Fase 4: permanece sem aprovação de aparelho para a nova configuração.

## Próxima validação no aparelho

1. Instalar APK debug atualizado e baixar/ativar Gemma em Configurações → Inteligência Artificial.
2. Abrir um livro PT-BR com diálogo, iniciar áudio e verificar nos logs o agendamento e a conclusão de `narration-analysis`.
3. Confirmar que o áudio inicia mesmo antes da análise terminar e que não ocorre underrun.
4. Após plano persistir, reconstruir a reprodução e confirmar que o speaker Kokoro selecionado muda apenas nos segmentos com confiança suficiente.
5. Medir PSS, primeira frase, underruns e transição de capítulo com tela apagada.
